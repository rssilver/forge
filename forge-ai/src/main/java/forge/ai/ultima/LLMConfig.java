package forge.ai.ultima;

/**
 * Configuration for LLM integration with Ultima AI.
 * Supports any OpenAI-compatible endpoint (LM Studio, Ollama, etc.).
 */
public class LLMConfig {
    private String endpoint = "http://localhost:1234/v1";
    private String model = "";
    private double temperature = 0.1;
    private int timeoutSeconds = 15;
    private boolean enabled = false;
    private boolean consultAtKeyDecisions = true;

    public LLMConfig() {}

    // Getters and setters
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public int getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public boolean isConsultAtKeyDecisions() { return consultAtKeyDecisions; }
    public void setConsultAtKeyDecisions(boolean consultAtKeyDecisions) { this.consultAtKeyDecisions = consultAtKeyDecisions; }

    /** Returns the chat completions URL for this endpoint. */
    public String getChatCompletionsUrl() {
        String base = endpoint.endsWith("/") ? endpoint.substring(0, endpoint.length() - 1) : endpoint;
        return base + "/chat/completions";
    }

    @Override
    public String toString() {
        return "LLMConfig{endpoint='" + endpoint + "', model='" + model + "', temp=" + temperature + ", timeout=" + timeoutSeconds + "s, enabled=" + enabled + "}";
    }
}
