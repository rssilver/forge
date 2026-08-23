package forge.ai.ultima;

import forge.ai.AiProfileUtil;
import forge.ai.AiProps;
import forge.ai.simulation.GameStateEvaluator;
import forge.ai.simulation.SpellAbilityPicker;
import forge.game.Game;
import forge.game.card.Card;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Main controller for the Ultima AI. Integrates alpha-beta pruning,
 * combo detection, and optional LLM consultation into a unified decision engine.
 */
public class UltimaController {

    private final Player player;
    private final Game game;
    private final int searchDepth;
    private final boolean useOpponentModeling;
    private final ComboDetector comboDetector = new ComboDetector();

    // LLM integration (lazy-initialized)
    private LLMClient llmClient;
    private LLMConfig llmConfig;

    // Cache to avoid redundant LLM queries on similar states
    private static final ConcurrentHashMap<String, UltimaLLMResponse> responseCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 500;

    public UltimaController(Player player) {
        this.player = player;
        this.game = player.getGame();
        this.searchDepth = AiProfileUtil.getIntProperty(player, AiProps.ULTIMA_ALPHA_BETA_DEPTH);
        this.useOpponentModeling = AiProfileUtil.getBoolProperty(player, AiProps.ULTIMA_OPPONENT_MODELING);
    }

    /** Choose the best spell ability to play using Ultima's decision engine. */
    public SpellAbility chooseBestSpell() {
        // 1. Get candidate moves from standard AI
        SpellAbilityPicker picker = new SpellAbilityPicker(player);
        List<SpellAbility> candidates = picker.getCandidateSpellsAndAbilities();

        if (candidates.isEmpty()) {
            return null;
        }

        // If only one option, take it immediately
        if (candidates.size() == 1) {
            return candidates.get(0);
        }

        // 2. Run alpha-beta search
        AlphaBetaEngine engine = new AlphaBetaEngine(player, searchDepth);
        AlphaBetaEngine.SearchResult result;

        if (useOpponentModeling && searchDepth >= 2) {
            result = engine.searchWithOpponentResponse();
        } else {
            result = engine.search();
        }

        // 3. Check for combos that might change the decision
        List<ComboDetector.ComboAlert> alerts = ComboDetector.detectCombos(game, player);
        if (!alerts.isEmpty()) {
            // Boost moves related to detected combos
            SpellAbility comboMove = findComboRelevantMove(candidates, alerts);
            if (comboMove != null) {
                return comboMove;
            }
        }

        // 4. Consult LLM at key decision points
        if (shouldConsultLLM()) {
            UltimaLLMResponse llmAdvice = getLLMAdvice();
            if (llmAdvice != null && result.bestMove() != null) {
                // If LLM strongly recommends something different, consider it
                SpellAbility llmSuggested = findMatchingSpell(candidates, llmAdvice.getRecommendedAction());
                if (llmSuggested != null && !llmSuggested.equals(result.bestMove().sa())) {
                    // Give LLM suggestion a bonus — if it's close to the alpha-beta best, use it
                    GameStateEvaluator.Score llmScore = evaluateSpell(llmSuggested);
                    if (llmScore.value >= result.score() - 100) {
                        return llmSuggested;
                    }
                }
            }
        }

        return result.bestMove() != null ? result.bestMove().sa() : candidates.get(0);
    }

    /** Evaluate a specific spell ability's score. */
    private GameStateEvaluator.Score evaluateSpell(SpellAbility sa) {
        AlphaBetaEngine engine = new AlphaBetaEngine(player, 1);
        int s = engine.search().score();
        return new GameStateEvaluator.Score(s);
    }

    /** Find a move that would activate or benefit from detected combos. */
    private SpellAbility findComboRelevantMove(
            List<SpellAbility> candidates, List<ComboDetector.ComboAlert> alerts) {
        for (var alert : alerts) {
            if (alert.severity() >= 70) {
                // High-severity combo — look for moves that set it up
                String[] keywords = alert.description().split(" ");
                for (SpellAbility sa : candidates) {
                    String name = sa.getHostCard().getName();
                    for (String keyword : keywords) {
                        if (name.contains(keyword)) {
                            return sa;
                        }
                    }
                }
            }
        }
        return null;
    }

    /** Determine whether to consult the LLM at this decision point. */
    private boolean shouldConsultLLM() {
        String endpoint = AiProfileUtil.getProperty(player, AiProps.ULTIMA_LLM_ENDPOINT);
        String model = AiProfileUtil.getProperty(player, AiProps.ULTIMA_LLM_MODEL);
        if (endpoint.isEmpty() || model.isEmpty()) return false;

        PhaseType phase = game.getPhaseHandler().getPhase();

        // Consult at key moments:
        // - Turn 1 (deck strategy read)
        // - When multiple tutor options exist
        // - End of turn before passing priority
        // - When combo detection flags something
        if (game.getPhaseHandler().getTurn() == 1 && !phase.isBefore(PhaseType.MAIN1)) return true;
        if (ComboDetector.hasActiveComboThreats(game, player)) return true;

        // Consult at end of turn to check for missed opportunities
        if (phase.isAfter(PhaseType.MAIN2)) return true;

        return false;
    }

    /** Get LLM advice for the current game state. */
    private UltimaLLMResponse getLLMAdvice() {
        String endpoint = AiProfileUtil.getProperty(player, AiProps.ULTIMA_LLM_ENDPOINT);
        String model = AiProfileUtil.getProperty(player, AiProps.ULTIMA_LLM_MODEL);
        double temp;
        try {
            temp = Double.parseDouble(AiProfileUtil.getProperty(player, AiProps.ULTIMA_LLM_TEMPERATURE));
        } catch (NumberFormatException e) {
            temp = 0.1;
        }
        int timeout;
        try {
            timeout = Integer.parseInt(AiProfileUtil.getProperty(player, AiProps.ULTIMA_LLM_TIMEOUT_SECONDS));
        } catch (NumberFormatException e) {
            timeout = 15;
        }

        llmConfig = new LLMConfig();
        llmConfig.setEndpoint(endpoint);
        llmConfig.setModel(model);
        llmConfig.setTemperature(temp);
        llmConfig.setTimeoutSeconds(timeout);
        llmConfig.setEnabled(true);

        if (llmClient == null) {
            llmClient = new LLMClient(llmConfig);
        }

        // Build cache key from game state summary
        String cacheKey = buildStateHash();
        UltimaLLMResponse cached = responseCache.get(cacheKey);
        if (cached != null) return cached;

        // Build game state description for the LLM
        String gameStateDesc = buildGameStateDescription();

        UltimaLLMResponse response = llmClient.query(gameStateDesc);
        if (response != null) {
            // Evict oldest entry if cache is full
            if (responseCache.size() >= MAX_CACHE_SIZE) {
                responseCache.clear(); // simple eviction strategy
            }
            responseCache.put(cacheKey, response);
        }

        return response;
    }

    /** Build a hashable summary of the current game state for caching. */
    private String buildStateHash() {
        StringBuilder sb = new StringBuilder();
        sb.append(game.getPhaseHandler().getTurn()).append(":");
        sb.append(game.getPhaseHandler().getPhase()).append(":");
        sb.append(player.getLife()).append(":");

        var hand = player.getCardsIn(forge.game.zone.ZoneType.Hand);
        for (Card c : hand) {
            sb.append(c.getName()).append(",");
        }

        var battlefield = player.getCardsIn(forge.game.zone.ZoneType.Battlefield);
        for (Card c : battlefield) {
            sb.append(c.getName()).append("/").append(c.getNetPower())
              .append(":").append(c.getNetToughness()).append(",");
        }

        return sb.toString().hashCode() + "";
    }

    /** Build a human-readable game state description for the LLM. */
    private String buildGameStateDescription() {
        StringBuilder sb = new StringBuilder();

        // Phase and turn info
        sb.append("Turn: ").append(game.getPhaseHandler().getTurn())
          .append(", Phase: ").append(game.getPhaseHandler().getPhase()).append("\n");

        // Player life totals
        for (var p : game.getPlayers()) {
            sb.append(p.getName()).append(": ").append(p.getLife()).append(" life\n");
        }

        // My hand summary
        var hand = player.getCardsIn(forge.game.zone.ZoneType.Hand);
        sb.append("\nMy hand (").append(hand.size()).append(" cards):\n");
        for (Card c : hand) {
            sb.append("  ").append(c.getName());
            if (c.isCreature()) {
                sb.append(" [")
                  .append(c.getCMC()).append("/")
                  .append(c.getType().toString())
                  .append("]");
            } else {
                sb.append(" [").append(c.getCMC()).append("]");
            }
            sb.append("\n");
        }

        // My battlefield
        var battlefield = player.getCardsIn(forge.game.zone.ZoneType.Battlefield);
        sb.append("\nMy battlefield (").append(battlefield.size()).append(" permanents):\n");
        for (Card c : battlefield) {
            sb.append("  ").append(c.getName());
            if (c.isCreature()) {
                sb.append(" ").append(c.getNetPower()).append("/").append(c.getNetToughness());
            }
            sb.append("\n");
        }

        // Opponent info
        for (var opp : player.getOpponents()) {
            var oppBattlefield = opp.getCardsIn(forge.game.zone.ZoneType.Battlefield);
            sb.append("\n").append(opp.getName()).append("'s battlefield (").append(oppBattlefield.size()).append("):\n");
            for (Card c : oppBattlefield) {
                sb.append("  ").append(c.getName());
                if (c.isCreature()) {
                    sb.append(" ").append(c.getNetPower()).append("/").append(c.getNetToughness());
                }
                sb.append("\n");
            }
        }

        // Combo alerts
        List<ComboDetector.ComboAlert> alerts = ComboDetector.detectCombos(game, player);
        if (!alerts.isEmpty()) {
            sb.append("\nCombo alerts:\n");
            for (var alert : alerts) {
                sb.append("  [").append(alert.severity()).append("/100] ").append(alert.description()).append("\n");
            }
        }

        return sb.toString();
    }

    /** Find a spell matching the LLM's recommendation. */
    private SpellAbility findMatchingSpell(List<SpellAbility> candidates, String recommendation) {
        if (recommendation == null || recommendation.isEmpty()) return null;

        String[] words = recommendation.toLowerCase().split("\\s+");
        int bestScore = 0;
        SpellAbility bestMatch = null;

        for (SpellAbility sa : candidates) {
            String name = sa.getHostCard().getName().toLowerCase();
            int score = 0;
            for (String word : words) {
                if (name.contains(word)) score += 10;
            }
            // Also check description keywords
            String desc = sa.getDescription().toLowerCase();
            for (String word : words) {
                if (desc.contains(word)) score += 3;
            }

            if (score > bestScore) {
                bestScore = score;
                bestMatch = sa;
            }
        }

        return bestScore >= 10 ? bestMatch : null;
    }

    /** Clear the LLM response cache. Call between games. */
    public static void clearCache() {
        responseCache.clear();
    }
}
