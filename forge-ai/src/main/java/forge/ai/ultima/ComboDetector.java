package forge.ai.ultima;

import forge.game.Game;
import forge.game.card.Card;
import forge.game.card.CardCollectionView;
import forge.game.player.Player;
import forge.game.spellability.SpellAbility;

import java.util.ArrayList;
import java.util.List;

/**
 * Detects potential combos and infinite loops on the battlefield.
 * Scans active cards for interacting trigger chains before they fire.
 */
public class ComboDetector {

    /** A detected combo or interaction pattern. */
    public record ComboAlert(String type, String description, int severity) {}

    private static final int HIGH_SEVERITY = 70;
    private static final int MEDIUM_SEVERITY = 60;
    private static final int LOW_SEVERITY = 50;

    /**
     * Scan the game state for potential combos involving the given player's cards.
     * Returns a list of alerts sorted by severity (highest first).
     */
    public static List<ComboAlert> detectCombos(Game game, Player player) {
        List<ComboAlert> alerts = new ArrayList<>();

        var battlefield = player.getCardsIn(forge.game.zone.ZoneType.Battlefield);
        var hand = player.getCardsIn(forge.game.zone.ZoneType.Hand);

        // Check for life gain/loss loops
        alerts.addAll(detectLifeLoops(battlefield, hand));

        // Check for token generation chains
        alerts.addAll(detectTokenChains(battlefield, hand));

        // Check for commander ability stacking (Commander format)
        alerts.addAll(detectCommanderStacking(game, player, battlefield, hand));

        // Sort by severity descending
        alerts.sort((a, b) -> Integer.compare(b.severity(), a.severity()));

        return alerts;
    }

    /**
     * Detect life gain <-> life loss infinite loops.
     */
    private static List<ComboAlert> detectLifeLoops(
            CardCollectionView battlefield, CardCollectionView hand) {
        List<ComboAlert> alerts = new ArrayList<>();

        boolean hasGainTriggersOnBattlefield = false;
        boolean hasLossTriggersOnBattlefield = false;
        String gainCardName = null;
        String lossCardName = null;

        for (Card c : battlefield) {
            if (hasLifeGainAbility(c)) {
                hasGainTriggersOnBattlefield = true;
                gainCardName = c.getName();
            }
            if (hasLoseLifeAbility(c)) {
                hasLossTriggersOnBattlefield = true;
                lossCardName = c.getName();
            }
        }

        for (Card c : hand) {
            boolean handHasGain = hasLifeGainAbility(c);
            boolean handHasLoss = hasLoseLifeAbility(c);

            if (handHasGain && handHasLoss) {
                alerts.add(new ComboAlert("life_loop_risk",
                    c.getName() + " can trigger a life gain/loss loop when cast.", 90));
            }

            if (hasGainTriggersOnBattlefield && handHasLoss) {
                alerts.add(new ComboAlert("life_chain",
                    c.getName() + " can chain with " + gainCardName + ".", MEDIUM_SEVERITY));
            }

            if (hasLossTriggersOnBattlefield && handHasGain) {
                alerts.add(new ComboAlert("life_chain",
                    c.getName() + " can chain with " + lossCardName + ".", MEDIUM_SEVERITY));
            }
        }

        if (hasGainTriggersOnBattlefield && hasLossTriggersOnBattlefield) {
            alerts.add(new ComboAlert("active_loop",
                gainCardName + " and " + lossCardName + " may be in a life loop.", 80));
        }

        return alerts;
    }

    /** Detect token generation chains. */
    private static List<ComboAlert> detectTokenChains(
            CardCollectionView battlefield, CardCollectionView hand) {
        List<ComboAlert> alerts = new ArrayList<>();

        for (Card c : battlefield) {
            if (createsTokens(c)) {
                for (Card hc : hand) {
                    if (createsTokens(hc)) {
                        alerts.add(new ComboAlert("token_chain",
                            hc.getName() + " can chain with " + c.getName(), LOW_SEVERITY));
                    }
                }
            }
        }

        return alerts;
    }

    /** Detect commander ability stacking opportunities. */
    private static List<ComboAlert> detectCommanderStacking(
            Game game, Player player, CardCollectionView battlefield, CardCollectionView hand) {
        List<ComboAlert> alerts = new ArrayList<>();

        for (Card c : battlefield) {
            if (c.isCommander()) {
                int abilityCount = countAbilities(c);

                for (Card hc : hand) {
                    boolean copies = hasCopyAbility(hc);
                    boolean reactivates = hasUntapAbility(hc);

                    if (copies && abilityCount > 0) {
                        alerts.add(new ComboAlert("commander_copy",
                            hc.getName() + " can copy " + c.getName(), HIGH_SEVERITY));
                    }
                    if (reactivates && abilityCount > 1) {
                        alerts.add(new ComboAlert("commander_reuse",
                            hc.getName() + " can reactivate " + c.getName(), MEDIUM_SEVERITY));
                    }
                }
            }
        }

        return alerts;
    }

    /** Check if a card has life gain abilities. */
    private static boolean hasLifeGainAbility(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            String desc = sa.getDescription();
            if ((desc.contains("gain") && desc.contains("life"))
                    || desc.contains("heal")) {
                return true;
            }
        }
        return false;
    }

    /** Check if a card has lose life abilities. */
    private static boolean hasLoseLifeAbility(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            String desc = sa.getDescription();
            if (desc.contains("lose") && desc.contains("life")) {
                return true;
            }
        }
        return false;
    }

    /** Check if a card creates tokens. */
    private static boolean createsTokens(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            String desc = sa.getDescription();
            if (desc.contains("token")) {
                return true;
            }
        }
        return false;
    }

    /** Count abilities on a card. */
    private static int countAbilities(Card card) {
        return card.getSpellAbilities().size()
                + card.getStaticAbilities().size();
    }

    /** Check if a card has copy/clone abilities. */
    private static boolean hasCopyAbility(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            String desc = sa.getDescription();
            if (desc.contains("copy") || desc.contains("Clone")) {
                return true;
            }
        }
        return false;
    }

    /** Check if a card has untap abilities. */
    private static boolean hasUntapAbility(Card card) {
        for (SpellAbility sa : card.getSpellAbilities()) {
            String desc = sa.getDescription();
            if (desc.contains("untap") || desc.contains("Untap")) {
                return true;
            }
        }
        return false;
    }

    /** Quick check: are there active combo threats on the board right now? */
    public static boolean hasActiveComboThreats(Game game, Player player) {
        List<ComboAlert> alerts = detectCombos(game, player);
        return alerts.stream().anyMatch(a -> a.severity() >= HIGH_SEVERITY);
    }

    /** Get combo alerts as a human-readable string for LLM context. */
    public static String getComboAlertsText(Game game, Player player) {
        List<ComboAlert> alerts = detectCombos(game, player);
        if (alerts.isEmpty()) return "No combos detected.";
        StringBuilder sb = new StringBuilder("Active combo alerts:\n");
        for (ComboAlert a : alerts) {
            sb.append(String.format("  [%s] %s\n", a.type(), a.description()));
        }
        return sb.toString();
    }
}
