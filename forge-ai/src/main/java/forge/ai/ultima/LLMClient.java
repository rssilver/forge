package forge.ai.ultima;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * HTTP client for OpenAI-compatible chat completion endpoints.
 * Works with LM Studio, Ollama, OpenAI, and any compatible API server.
 */
public class LLMClient {

    private final LLMConfig config;

    public LLMClient(LLMConfig config) {
        this.config = config;
    }

    /**
     * Send a game state to the LLM and get back a decision recommendation.
     * Returns null if LLM is disabled, unreachable, or times out.
     */
    public UltimaLLMResponse query(String gameStateDescription) {
        if (!config.isEnabled() || config.getModel().isEmpty()) {
            return null;
        }

        String prompt = buildPrompt(gameStateDescription);
        return chatCompletion(prompt);
    }

    /**
     * Batch query: send multiple game states and get recommendations for all.
     */
    public List<UltimaLLMResponse> batchQuery(List<String> descriptions) {
        if (descriptions.isEmpty()) return List.of();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < descriptions.size(); i++) {
            if (i > 0) sb.append("\n---\n");
            sb.append(descriptions.get(i));
        }
        UltimaLLMResponse response = query(sb.toString());
        return response != null ? List.of(response) : List.of();
    }

    private String buildPrompt(String gameStateDescription) {
        return """
                You are an expert Magic: The Gathering strategist analyzing a game state.
                
                Game State:
                %s
                
                Provide your analysis in JSON format with the following fields:
                - recommended_action: A concise description of the best move
                - reasoning: Why this is the best move (1-2 sentences)
                - combo_alerts: Array of potential combos or infinite loops to watch for
                - priority_targets: Array of important targets on the board
                
                Return ONLY valid JSON, no markdown formatting.
                """.formatted(gameStateDescription);
    }

    private UltimaLLMResponse chatCompletion(String prompt) {
        try {
            URI uri = new URI(config.getChatCompletionsUrl());
            HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(config.getTimeoutSeconds() * 1000);
            conn.setReadTimeout(config.getTimeoutSeconds() * 1000);
            conn.setRequestProperty("Content-Type", "application/json");
            if (!config.getApiKey().isEmpty()) {
                conn.setRequestProperty("Authorization", "Bearer " + config.getApiKey());
            }
            conn.setDoOutput(true);

            String body = buildRequestBody(prompt);
            try (var os = conn.getOutputStream()) {
                os.write(body.getBytes());
            }

            int code = conn.getResponseCode();
            if (code != 200) {
                return readErrorMessage(conn);
            }

            return parseResponse(conn);

        } catch (Exception e) {
            System.err.println("Ultima LLM query failed: " + e.getMessage());
            return null;
        }
    }

    private String buildRequestBody(String prompt) {
        List<String> models = List.of(config.getModel(), "default", "llama3");
        String modelToUse = models.stream().filter(m -> !m.isEmpty()).findFirst().orElse("default");

        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"").append(modelToUse).append("\",");
        sb.append("\"temperature\":").append(config.getTemperature()).append(",");
        sb.append("\"messages\":[{\"role\":\"user\",\"content\":\"").append(escapeJson(prompt)).append("\"}]}");
        return sb.toString();
    }

    private UltimaLLMResponse parseResponse(HttpURLConnection conn) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return parseJson(sb.toString());
        }
    }

    private UltimaLLMResponse readErrorMessage(HttpURLConnection conn) throws IOException {
        // Non-2xx responses write their body to the *error* stream; getInputStream() would throw.
        java.io.InputStream in = conn.getErrorStream();
        if (in == null) {
            return new UltimaLLMResponse("No recommendation", "LLM returned no error detail", List.of(), List.of());
        }
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return parseJson(sb.toString());
        }
    }

    /** Parse JSON response into UltimaLLMResponse. Handles basic JSON parsing without external deps. */
    private UltimaLLMResponse parseJson(String json) {
        try {
            // Extract content from the response - look for "content" field
            String content = extractStringField(json, "content");
            if (content == null || content.isEmpty()) {
                return new UltimaLLMResponse("No recommendation", "LLM returned empty response", List.of(), List.of());
            }

            // Try to parse the JSON within the content
            String recommendedAction = extractStringField(content, "recommended_action");
            String reasoning = extractStringField(content, "reasoning");
            List<String> comboAlerts = extractStringArray(content, "combo_alerts");
            List<String> priorityTargets = extractStringArray(content, "priority_targets");

            return new UltimaLLMResponse(
                recommendedAction != null ? recommendedAction : "No specific recommendation",
                reasoning != null ? reasoning : content.substring(0, Math.min(200, content.length())),
                comboAlerts, priorityTargets
            );
        } catch (Exception e) {
            return new UltimaLLMResponse("Parse error: " + e.getMessage(), json, List.of(), List.of());
        }
    }

    private String extractStringField(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return null;
        int colonIdx = json.indexOf(':', idx + key.length());
        if (colonIdx == -1) return null;
        int quoteStart = json.indexOf('"', colonIdx + 1);
        if (quoteStart == -1) return null;
        int quoteEnd = findMatchingQuote(json, quoteStart);
        if (quoteEnd == -1) return null;
        return unescapeJson(json.substring(quoteStart + 1, quoteEnd));
    }

    private List<String> extractStringArray(String json, String field) {
        String key = "\"" + field + "\"";
        int idx = json.indexOf(key);
        if (idx == -1) return List.of();
        int bracketStart = json.indexOf('[', idx);
        if (bracketStart == -1) return List.of();
        int bracketEnd = findMatchingBracket(json, bracketStart);
        if (bracketEnd == -1) return List.of();
        String arrayContent = json.substring(bracketStart + 1, bracketEnd);
        List<String> result = new ArrayList<>();
        for (String item : arrayContent.split(",")) {
            int q1 = item.indexOf('"');
            if (q1 == -1) continue;
            int q2 = item.indexOf('"', q1 + 1);
            if (q2 == -1) continue;
            result.add(unescapeJson(item.substring(q1 + 1, q2)));
        }
        return result;
    }

    private int findMatchingQuote(String s, int start) {
        for (int i = start + 1; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\') {
                i++; // skip the escaped character so \" doesn't end the string early
            } else if (c == '"') {
                return i;
            }
        }
        return -1;
    }

    private int findMatchingBracket(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return i; }
        }
        return -1;
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescapeJson(String s) {
        return s.replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
