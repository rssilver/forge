package forge.ai;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.List;

import com.google.common.collect.Lists;

import forge.GuiDesktop;
import forge.deck.Deck;
import forge.game.Game;
import forge.game.GameRules;
import forge.game.GameStage;
import forge.game.GameType;
import forge.game.Match;
import forge.game.phase.PhaseType;
import forge.game.player.Player;
import forge.game.player.RegisteredPlayer;
import forge.gui.GuiBase;
import forge.localinstance.properties.ForgePreferences;
import forge.localinstance.properties.ForgePreferences.FPref;
import forge.model.FModel;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * End-to-end bridge test for the Ultima LLM settings.
 *
 * <p>This is the seam where the original bug lived: the GUI saved an Ultima LLM API key, but the
 * AI side never read it because {@code AiProps} had no matching entry and nothing copied the GUI
 * preference into a place the AI could see. The fix wires the value through three hops:
 * <ol>
 *   <li>User sets a value in Preferences ({@link ForgePreferences.FPref}).</li>
 *   <li>{@link FModel#syncUltimaLlmProperties()} copies it to a system property.</li>
 *   <li>{@link AiProfileUtil#getProperty(Player, AiProps)} reads that system property back.</li>
 * </ol>
 *
 * <p>The test drives all three hops with real objects (no mocks) and asserts the value survives the
 * round trip. It also guards the implicit string contract between the two modules: FModel writes
 * {@code forge.ai.<propname>} while AiProfileUtil derives the same key from the enum name, so a
 * rename on either side would silently break the bridge again.
 */
public class UltimaLlmSettingsBridgeTest {

    private static boolean initialized = false;

    @BeforeMethod
    public void initializeModel() {
        if (initialized) {
            return;
        }
        GuiBase.setInterface(new GuiDesktop());
        FModel.initialize(null, preferences -> null);
        initialized = true;
    }

    /** Builds a real Player backed by an AI lobby player with no explicit profile. */
    private static Player buildAiPlayer() {
        List<RegisteredPlayer> players = Lists.newArrayList();
        Deck deck = new Deck();
        players.add(new RegisteredPlayer(deck).setPlayer(new LobbyPlayerAi("ai", null)));
        GameRules rules = new GameRules(GameType.Constructed);
        Match match = new Match(rules, players, "Test");
        Game game = new Game(players, rules, match);
        Player p = game.getPlayers().get(0);
        game.setAge(GameStage.Play);
        game.getPhaseHandler().devModeSet(PhaseType.MAIN1, p);
        return p;
    }

    /** Sets a GUI preference and pushes it through the FModel bridge. */
    private static void setAndSync(FPref pref, String value) {
        ForgePreferences prefs = FModel.getPreferences();
        prefs.setPref(pref, value);
        FModel.syncUltimaLlmProperties();
    }

    @Test
    public void apiKeySurvivesGuiToAiRoundTrip() {
        Player p = buildAiPlayer();
        setAndSync(FPref.ULTIMA_LLM_API_KEY, "sk-test-12345");

        String seenByAi = AiProfileUtil.getProperty(p, AiProps.ULTIMA_LLM_API_KEY);
        assertEquals(seenByAi, "sk-test-12345",
                "The API key set in the GUI must be readable by the AI side (the original bug).");
    }

    @Test
    public void endpointAndModelSurviveGuiToAiRoundTrip() {
        Player p = buildAiPlayer();
        setAndSync(FPref.ULTIMA_LLM_ENDPOINT, "http://localhost:11434/v1/chat/completions");
        setAndSync(FPref.ULTIMA_LLM_MODEL, "qwen3:8b");

        assertEquals(AiProfileUtil.getProperty(p, AiProps.ULTIMA_LLM_ENDPOINT),
                "http://localhost:11434/v1/chat/completions", "Endpoint must round-trip.");
        assertEquals(AiProfileUtil.getProperty(p, AiProps.ULTIMA_LLM_MODEL),
                "qwen3:8b", "Model must round-trip.");
    }

    @Test
    public void emptyGuiPrefFallsBackToDefault() {
        Player p = buildAiPlayer();
        setAndSync(FPref.ULTIMA_LLM_API_KEY, "");

        // Empty GUI value should not clobber the enum default (which is also empty for the key).
        String seenByAi = AiProfileUtil.getProperty(p, AiProps.ULTIMA_LLM_API_KEY);
        assertEquals(seenByAi, AiProps.ULTIMA_LLM_API_KEY.getDefault(),
                "An empty GUI value should resolve to the hardcoded default.");
    }

    @Test
    public void systemPropertyKeyMatchesEnumDerivedKey() {
        // Guards the implicit cross-module string contract: FModel writes forge.ai.<name> and
        // AiProfileUtil derives the same key from the enum. If they drift, the bridge breaks.
        setAndSync(FPref.ULTIMA_LLM_API_KEY, "sk-contract-check");

        String expectedKey = "forge.ai." + AiProps.ULTIMA_LLM_API_KEY.name().toLowerCase();
        assertEquals(System.getProperty(expectedKey), "sk-contract-check",
                "FModel must write to the exact system property key that AiProfileUtil reads.");
    }

    @Test
    public void guiOverrideBeatsDefaultWhenProfileIsUnset() {
        // With no explicit AI profile, getAIProp returns "" (== default), so a GUI override wins.
        Player p = buildAiPlayer();
        setAndSync(FPref.ULTIMA_LLM_TEMPERATURE, "0.7");

        String seenByAi = AiProfileUtil.getProperty(p, AiProps.ULTIMA_LLM_TEMPERATURE);
        assertEquals(seenByAi, "0.7",
                "A GUI temperature override must beat the default when no profile pins it.");
        assertTrue(!seenByAi.equals(AiProps.ULTIMA_LLM_TEMPERATURE.getDefault()),
                "The resolved value should differ from the hardcoded default (proving the override won).");
    }
}
