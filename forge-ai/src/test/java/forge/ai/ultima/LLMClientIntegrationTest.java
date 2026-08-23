package forge.ai.ultima;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Integration test for {@link LLMClient} against a local in-process HTTP server.
 * Verifies the request is well-formed (especially the Authorization header, which was
 * previously dropped when no API key was configured) and that responses parse correctly.
 */
public class LLMClientIntegrationTest {

    private HttpServer server;
    private volatile String capturedAuth;
    private volatile String capturedContentType;
    private volatile String capturedBody;
    private CountDownLatch requestReceived = new CountDownLatch(1);

    @BeforeClass
    public void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.setExecutor(Executors.newCachedThreadPool());
        server.createContext("/v1/chat/completions", this::handle);
        server.start();
    }

    @AfterClass
    public void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @BeforeMethod
    public void resetCapture() {
        capturedAuth = null;
        capturedContentType = null;
        capturedBody = null;
        requestReceived = new CountDownLatch(1);
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            capturedAuth = exchange.getRequestHeaders().getFirst("Authorization");
            capturedContentType = exchange.getRequestHeaders().getFirst("Content-Type");
            try (InputStream in = exchange.getRequestBody()) {
                capturedBody = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }

            String inner = "{\"recommended_action\":\"Cast Lightning Bolt\","
                    + "\"reasoning\":\"Best direct damage\","
                    + "\"combo_alerts\":[\"infinite loop\"],"
                    + "\"priority_targets\":[\"opponent creature\"]}";
            // OpenAI-style envelope; LLMClient.parseJson only needs a "content" field.
            String response = "{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\""
                    + inner.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}}]}";

            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        } finally {
            requestReceived.countDown();
        }
    }

    private LLMConfig config() {
        int port = server.getAddress().getPort();
        LLMConfig c = new LLMConfig();
        c.setEndpoint("http://localhost:" + port + "/v1");
        c.setApiKey("sk-test-key");
        c.setModel("test-model");
        c.setEnabled(true);
        return c;
    }

    @Test
    public void testQuerySendsAuthorizationHeaderAndParsesResponse() throws Exception {
        UltimaLLMResponse resp = new LLMClient(config()).query("It is my turn with a 5/5 on the battlefield.");

        Assert.assertTrue(requestReceived.await(5, TimeUnit.SECONDS), "server should have received the request");
        // The core regression: the API key must be sent as a Bearer token.
        Assert.assertEquals(capturedAuth, "Bearer sk-test-key");
        Assert.assertEquals(capturedContentType, "application/json");
        Assert.assertTrue(capturedBody.contains("\"model\":\"test-model\""), "body should name the model");
        Assert.assertTrue(capturedBody.contains("\"role\":\"user\""), "body should contain a user message");

        // Response parsing.
        Assert.assertNotNull(resp);
        Assert.assertEquals(resp.getRecommendedAction(), "Cast Lightning Bolt");
        Assert.assertEquals(resp.getReasoning(), "Best direct damage");
        Assert.assertEquals(resp.getComboAlerts().size(), 1);
        Assert.assertEquals(resp.getComboAlerts().get(0), "infinite loop");
        Assert.assertEquals(resp.getPriorityTargets().get(0), "opponent creature");
    }

    @Test
    public void testQueryDisabledReturnsNullWithoutRequest() {
        LLMConfig c = config();
        c.setEnabled(false);
        UltimaLLMResponse resp = new LLMClient(c).query("state");
        Assert.assertNull(resp, "disabled client should return null without calling the server");
        Assert.assertNull(capturedAuth, "no request should have been sent when disabled");
    }

    @Test
    public void testQueryEmptyModelReturnsNull() {
        LLMConfig c = config();
        c.setModel("");
        UltimaLLMResponse resp = new LLMClient(c).query("state");
        Assert.assertNull(resp, "empty model should short-circuit to null");
    }

    @Test
    public void testQueryNoApiKeyStillSendsRequest() throws Exception {
        // Even with an empty API key (e.g. LM Studio), the request must still go out —
        // it just omits the Authorization header rather than failing entirely.
        LLMConfig c = config();
        c.setApiKey("");
        UltimaLLMResponse resp = new LLMClient(c).query("state");

        Assert.assertTrue(requestReceived.await(5, TimeUnit.SECONDS), "request should still be sent with no key");
        Assert.assertNull(capturedAuth, "no Authorization header expected when the API key is empty");
        Assert.assertNotNull(resp);
    }
}
