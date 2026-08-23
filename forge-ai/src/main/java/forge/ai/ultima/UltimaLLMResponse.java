package forge.ai.ultima;

import java.util.List;

/** Response from the LLM containing strategic recommendations. */
public class UltimaLLMResponse {
    private final String recommendedAction;
    private final String reasoning;
    private final List<String> comboAlerts;
    private final List<String> priorityTargets;

    public UltimaLLMResponse(String recommendedAction, String reasoning, List<String> comboAlerts, List<String> priorityTargets) {
        this.recommendedAction = recommendedAction;
        this.reasoning = reasoning;
        this.comboAlerts = comboAlerts;
        this.priorityTargets = priorityTargets;
    }

    public String getRecommendedAction() { return recommendedAction; }
    public String getReasoning() { return reasoning; }
    public List<String> getComboAlerts() { return comboAlerts; }
    public List<String> getPriorityTargets() { return priorityTargets; }

    @Override
    public String toString() {
        return "UltimaLLMResponse{action='" + recommendedAction + "', alerts=" + comboAlerts.size() + "}";
    }
}
