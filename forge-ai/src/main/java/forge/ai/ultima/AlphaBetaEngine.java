package forge.ai.ultima;

import forge.ai.simulation.GameCopier;
import forge.ai.simulation.GameStateEvaluator;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * Alpha-beta pruning search engine for Ultima AI.
 * Builds a shallow game tree and prunes branches that can't improve the current best score.
 */
public class AlphaBetaEngine {

    private static final int DEFAULT_MAX_DEPTH = 3;
    private static final int MAX_CANDIDATES_PER_NODE = 8; // limit branching factor for performance

    /** A move in the search tree: a spell ability to play and its chosen targets/modes. */
    public record SearchMove(SpellAbility sa, String description) {}

    /** Result of an alpha-beta search. */
    public record SearchResult(SearchMove bestMove, int score, int nodesVisited) {}

    private final Player player;
    private final Game game;
    private final GameStateEvaluator evaluator = new GameStateEvaluator();
    private int maxDepth;
    private int nodesVisited = 0;

    public AlphaBetaEngine(Player player, int maxDepth) {
        this.player = player;
        this.game = player.getGame();
        this.maxDepth = Math.min(maxDepth, DEFAULT_MAX_DEPTH); // cap to prevent runaway search
    }

    /**
     * Run alpha-beta search and return the best move.
     * Returns null if no good moves found (pass is better).
     */
    public SearchResult search() {
        nodesVisited = 0;

        List<SpellAbility> candidates = getCandidateMoves();
        if (candidates.isEmpty()) {
            return new SearchResult(null, getBaseScore(), 0);
        }

        // Limit branching factor — only evaluate top N candidates by heuristic score
        candidates = rankCandidates(candidates);
        if (candidates.size() > MAX_CANDIDATES_PER_NODE) {
            candidates = candidates.subList(0, MAX_CANDIDATES_PER_NODE);
        }

        int bestScore = Integer.MIN_VALUE;
        SearchMove bestMove = null;

        for (SpellAbility sa : candidates) {
            nodesVisited++;
            GameStateEvaluator.Score score = evaluateMove(sa);
            if (score.value > bestScore) {
                bestScore = score.value;
                bestMove = new SearchMove(sa, describeMove(sa));
            }
        }

        // If we found a move that improves our position, return it
        if (bestScore <= getBaseScore() && bestMove != null) {
            return new SearchResult(null, getBaseScore(), nodesVisited);
        }

        return new SearchResult(bestMove, bestScore, nodesVisited);
    }

    /**
     * Run alpha-beta search with opponent consideration.
     * Looks ahead: "if I play X, what will opponent do?"
     */
    public SearchResult searchWithOpponentResponse() {
        if (maxDepth < 2) {
            return search(); // shallow search without opponent modeling
        }

        nodesVisited = 0;

        List<SpellAbility> candidates = getCandidateMoves();
        if (candidates.isEmpty()) {
            return new SearchResult(null, getBaseScore(), 0);
        }

        candidates = rankCandidates(candidates);
        if (candidates.size() > MAX_CANDIDATES_PER_NODE) {
            candidates = candidates.subList(0, MAX_CANDIDATES_PER_NODE);
        }

        int alpha = Integer.MIN_VALUE;
        SearchMove bestMove = null;
        int bestScore = Integer.MIN_VALUE;

        for (SpellAbility sa : candidates) {
            nodesVisited++;
            // Simulate playing this move, then search opponent's response
            GameStateEvaluator.Score score = simulateAndSearch(sa, 1, alpha, Integer.MAX_VALUE);
            if (score.value > bestScore) {
                bestScore = score.value;
                bestMove = new SearchMove(sa, describeMove(sa));
                alpha = Math.max(alpha, score.value);
            }
        }

        return new SearchResult(bestMove, bestScore, nodesVisited);
    }

    /** Recursive alpha-beta search with opponent modeling. */
    private GameStateEvaluator.Score simulateAndSearch(SpellAbility sa, int depth, int alpha, int beta) {
        if (depth >= maxDepth) {
            // Leaf node — evaluate current state after playing this move
            return playMoveInCopy(sa);
        }

        // Copy game and play the move
        GameCopier copier = new GameCopier(game);
        Game simGame = copier.makeCopy(null, player);
        Player simPlayer = (Player) copier.find(player);

        // Play the spell ability in the copy
        SpellAbility simSa = findSpellInSimGame(sa, simGame);
        if (simSa != null) {
            simSa.setActivatingPlayer(simPlayer);
            playSpell(simGame, simPlayer, simSa);
        }

        // Get opponent's candidate moves at next depth
        Player opponent = simPlayer.getWeakestOpponent();
        List<SpellAbility> oppCandidates = getCandidateMovesFor(simGame, opponent);
        if (oppCandidates.isEmpty()) {
            return evaluator.getScoreForGameState(simGame, simPlayer);
        }

        // Evaluate opponent's responses (minimizing for us)
        int bestOppScore = Integer.MAX_VALUE;
        for (SpellAbility oppSa : oppCandidates.subList(0, Math.min(MAX_CANDIDATES_PER_NODE, oppCandidates.size()))) {
            nodesVisited++;
            SpellAbility simOppSa = findSpellInSimGame(oppSa, simGame);
            if (simOppSa != null) {
                simOppSa.setActivatingPlayer(opponent);
                playSpell(simGame, opponent, simOppSa);

                // Recurse: our response to opponent's move
                GameStateEvaluator.Score score = evaluateState(simGame, simPlayer, depth + 1);
                bestOppScore = Math.min(bestOppScore, score.value);

                // Alpha-beta pruning: if opponent found a good enough move, stop searching
                if (bestOppScore <= alpha) {
                    break; // beta cutoff
                }
            }
        }

        return new GameStateEvaluator.Score(depth % 2 == 0 ? -bestOppScore : bestOppScore);
    }

    /** Evaluate the game state from the player's perspective at a given depth. */
    private GameStateEvaluator.Score evaluateState(Game simGame, Player simPlayer, int depth) {
        if (depth >= maxDepth) {
            return evaluator.getScoreForGameState(simGame, simPlayer);
        }

        // Get our candidate responses
        List<SpellAbility> myResponses = getCandidateMovesFor(simGame, simPlayer);
        if (myResponses.isEmpty()) {
            return evaluator.getScoreForGameState(simGame, simPlayer);
        }

        int bestMyScore = Integer.MIN_VALUE;
        for (SpellAbility sa : myResponses.subList(0, Math.min(MAX_CANDIDATES_PER_NODE / 2, myResponses.size()))) {
            nodesVisited++;
            SpellAbility simSa = findSpellInSimGame(sa, simGame);
            if (simSa != null) {
                simSa.setActivatingPlayer(simPlayer);
                playSpell(simGame, simPlayer, simSa);
                GameStateEvaluator.Score score = evaluator.getScoreForGameState(simGame, simPlayer);
                bestMyScore = Math.max(bestMyScore, score.value);
            }
        }

        return new GameStateEvaluator.Score(bestMyScore == Integer.MIN_VALUE ? 0 : bestMyScore);
    }

    /** Evaluate a single move by playing it in a game copy. */
    private GameStateEvaluator.Score evaluateMove(SpellAbility sa) {
        GameCopier copier = new GameCopier(game);
        Game simGame = copier.makeCopy(null, player);
        Player simPlayer = (Player) copier.find(player);

        SpellAbility simSa = findSpellInSimGame(sa, simGame);
        if (simSa != null) {
            simSa.setActivatingPlayer(simPlayer);
            playSpell(simGame, simPlayer, simSa);
        }

        return evaluator.getScoreForGameState(simGame, simPlayer);
    }

    private GameStateEvaluator.Score playMoveInCopy(SpellAbility sa) {
        GameCopier copier = new GameCopier(game);
        Game simGame = copier.makeCopy(null, player);
        Player simPlayer = (Player) copier.find(player);

        SpellAbility simSa = findSpellInSimGame(sa, simGame);
        if (simSa != null) {
            simSa.setActivatingPlayer(simPlayer);
            playSpell(simGame, simPlayer, simSa);
        }

        return evaluator.getScoreForGameState(simGame, simPlayer);
    }

    /** Get all candidate moves for the player. */
    private List<SpellAbility> getCandidateMoves() {
        SpellAbilityPicker picker = new SpellAbilityPicker(player);
        return picker.getCandidateSpellsAndAbilities();
    }

    /** Get candidate moves for a specific player in a specific game state. */
    private List<SpellAbility> getCandidateMovesFor(Game g, Player p) {
        SpellAbilityPicker picker = new SpellAbilityPicker(p);
        return picker.getCandidateSpellsAndAbilities();
    }

    /** Rank candidates by heuristic score (fast evaluation before full simulation). */
    private List<SpellAbility> rankCandidates(List<SpellAbility> candidates) {
        // Quick sort: evaluate each in a shallow copy, sort descending
        List<int[]> scored = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            GameStateEvaluator.Score score = evaluateMove(candidates.get(i));
            scored.add(new int[]{i, score.value});
        }
        scored.sort((a, b) -> Integer.compare(b[1], a[1]));

        List<SpellAbility> ranked = new ArrayList<>();
        for (int[] entry : scored) {
            ranked.add(candidates.get(entry[0]));
        }
        return ranked;
    }

    /** Find the corresponding spell ability in a copied game. */
    private SpellAbility findSpellInSimGame(SpellAbility origSa, Game simGame) {
        Card host = origSa.getHostCard();
        for (Card c : simGame.getCardsIn(forge.game.zone.ZoneType.Hand)) {
            if (c.getName().equals(host.getName()) && !c.equals(host)) {
                for (SpellAbility sa : c.getSpellAbilities()) {
                    if (sa.getDescription().startsWith(origSa.getDescription())) {
                        return sa;
                    }
                }
            }
        }
        // Also check battlefield
        for (Card c : simGame.getCardsIn(forge.game.zone.ZoneType.Battlefield)) {
            if (c.getName().equals(host.getName()) && !c.equals(host)) {
                for (SpellAbility sa : c.getSpellAbilities()) {
                    if (sa.getDescription().startsWith(origSa.getDescription())) {
                        return sa;
                    }
                }
            }
        }
        return null;
    }

    /** Play a spell in the game state. */
    private void playSpell(Game g, Player p, SpellAbility sa) {
        // Resolve the spell and any triggered abilities
        forge.ai.simulation.GameSimulator.resolveStack(g, p.getWeakestOpponent());
    }

    /** Get base score of current game state without making a move. */
    private int getBaseScore() {
        return evaluator.getScoreForGameState(game, player).value;
    }

    private String describeMove(SpellAbility sa) {
        Card host = sa.getHostCard();
        if (sa.isSpell()) {
            return "Cast " + host.getName();
        }
        return "Activate " + host.getName() + ": " + sa.getDescription().split("\n")[0];
    }

    public int getNodesVisited() {
        return nodesVisited;
    }
}
