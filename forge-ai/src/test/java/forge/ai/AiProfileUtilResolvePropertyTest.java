package forge.ai;

import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Unit tests for {@link AiProfileUtil#resolveProperty(String, String, AiProps)}, the pure
 * precedence logic that decides which of a profile value, a live GUI setting (system property),
 * or the hardcoded default wins. Extracted from getProperty so it can be tested without a Player/Game.
 */
public class AiProfileUtilResolvePropertyTest {

    // ULTIMA_LLM_TEMPERATURE has default "0.1"
    private static final AiProps TEMP = AiProps.ULTIMA_LLM_TEMPERATURE;
    private static final String DEFAULT_TEMP = "0.1";

    @Test
    public void testNoProfileValue_UsesGuiSetting() {
        Assert.assertEquals(AiProfileUtil.resolveProperty(null, "0.5", TEMP), "0.5");
        Assert.assertEquals(AiProfileUtil.resolveProperty("", "0.9", TEMP), "0.9");
    }

    @Test
    public void testNoProfileValue_NoGuiSetting_UsesDefault() {
        Assert.assertEquals(AiProfileUtil.resolveProperty(null, null, TEMP), DEFAULT_TEMP);
        Assert.assertEquals(AiProfileUtil.resolveProperty("", "", TEMP), DEFAULT_TEMP);
    }

    @Test
    public void testExplicitProfileValue_WinsOverGuiAndDefault() {
        // Profile value differs from default -> profile wins even if a GUI setting exists.
        Assert.assertEquals(AiProfileUtil.resolveProperty("0.7", "0.5", TEMP), "0.7");
        Assert.assertEquals(AiProfileUtil.resolveProperty("0.7", null, TEMP), "0.7");
    }

    @Test
    public void testProfileValueEqualsDefault_GuiOverrideWins() {
        // The key regression: profile value == default should NOT block a GUI override.
        Assert.assertEquals(AiProfileUtil.resolveProperty(DEFAULT_TEMP, "0.5", TEMP), "0.5");
    }

    @Test
    public void testProfileValueEqualsDefault_NoGui_UsesDefault() {
        // Profile value == default and no GUI setting -> the (equal) default is returned.
        Assert.assertEquals(AiProfileUtil.resolveProperty(DEFAULT_TEMP, null, TEMP), DEFAULT_TEMP);
        Assert.assertEquals(AiProfileUtil.resolveProperty(DEFAULT_TEMP, "", TEMP), DEFAULT_TEMP);
    }

    @Test
    public void testDifferentPropsUseTheirOwnDefaults() {
        // ULTIMA_ALPHA_BETA_DEPTH default is "3"; a GUI override should win when profile == "3".
        AiProps depth = AiProps.ULTIMA_ALPHA_BETA_DEPTH;
        Assert.assertEquals(AiProfileUtil.resolveProperty("3", "5", depth), "5");
        Assert.assertEquals(AiProfileUtil.resolveProperty(null, null, depth), "3");
    }
}
