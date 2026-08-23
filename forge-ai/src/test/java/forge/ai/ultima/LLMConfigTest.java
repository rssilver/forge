package forge.ai.ultima;

import org.testng.Assert;
import org.testng.annotations.Test;

/** Unit tests for {@link LLMConfig} defaults and API-key normalization. */
public class LLMConfigTest {

    @Test
    public void testDefaults() {
        LLMConfig c = new LLMConfig();
        Assert.assertEquals(c.getEndpoint(), "http://localhost:1234/v1");
        Assert.assertEquals(c.getApiKey(), "");
        Assert.assertEquals(c.getModel(), "");
        Assert.assertEquals(c.getTemperature(), 0.1, 1e-9);
        Assert.assertEquals(c.getTimeoutSeconds(), 15);
        Assert.assertFalse(c.isEnabled());
        Assert.assertTrue(c.isConsultAtKeyDecisions());
    }

    @Test
    public void testApiKeyNullBecomesEmpty() {
        LLMConfig c = new LLMConfig();
        c.setApiKey(null);
        Assert.assertEquals(c.getApiKey(), "");
    }

    @Test
    public void testApiKeyIsTrimmed() {
        LLMConfig c = new LLMConfig();
        c.setApiKey("  sk-abc123  ");
        Assert.assertEquals(c.getApiKey(), "sk-abc123");
    }

    @Test
    public void testApiKeyPreserved() {
        LLMConfig c = new LLMConfig();
        c.setApiKey("sk-test-key");
        Assert.assertEquals(c.getApiKey(), "sk-test-key");
    }

    @Test
    public void testChatCompletionsUrlNoTrailingSlash() {
        LLMConfig c = new LLMConfig();
        c.setEndpoint("http://localhost:1234/v1");
        Assert.assertEquals(c.getChatCompletionsUrl(), "http://localhost:1234/v1/chat/completions");
    }

    @Test
    public void testChatCompletionsUrlStripsTrailingSlash() {
        LLMConfig c = new LLMConfig();
        c.setEndpoint("http://ollama.local:11434/v1/");
        Assert.assertEquals(c.getChatCompletionsUrl(), "http://ollama.local:11434/v1/chat/completions");
    }

    @Test
    public void testSettersRoundTrip() {
        LLMConfig c = new LLMConfig();
        c.setEndpoint("http://x:9/v2");
        c.setModel("llama3.1-8b");
        c.setTemperature(0.7);
        c.setTimeoutSeconds(45);
        c.setEnabled(true);
        c.setConsultAtKeyDecisions(false);

        Assert.assertEquals(c.getEndpoint(), "http://x:9/v2");
        Assert.assertEquals(c.getModel(), "llama3.1-8b");
        Assert.assertEquals(c.getTemperature(), 0.7, 1e-9);
        Assert.assertEquals(c.getTimeoutSeconds(), 45);
        Assert.assertTrue(c.isEnabled());
        Assert.assertFalse(c.isConsultAtKeyDecisions());
    }
}
