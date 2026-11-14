package com.haruhi.botserver.features.ai.client.deepseek;

import com.haruhi.botserver.configuration.service.Configs;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeepSeekClientTest {

    @TempDir
    Path dir;

    private HttpServer server;
    private final AtomicReference<String> chatBody = new AtomicReference<>();

    @BeforeEach
    void setUp() throws Exception {
        Path config = dir.resolve("config");
        Files.createDirectories(config);
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/chat/completions", exchange -> {
            chatBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            respond(exchange, 200, """
                    {"id":"x","model":"deepseek-flash","choices":[{"index":0,"message":{"role":"assistant","content":"{\\"type\\":\\"silent\\"}"},"finish_reason":"stop"}],"usage":{"prompt_tokens":10,"completion_tokens":2,"total_tokens":12}}
                    """);
        });
        server.createContext("/user/balance", exchange -> respond(exchange, 200, """
                {"is_available":true,"balance_infos":[{"currency":"CNY","total_balance":"10.00","granted_balance":"1.00","topped_up_balance":"9.00"}]}
                """));
        server.start();
        Files.writeString(config.resolve("ai.properties"), """
                ds.api.key=test-key
                ds.api.base_url=http://127.0.0.1:%d
                ds.api.timeout=5
                """.formatted(server.getAddress().getPort()));
        Configs.useConfigDirForTest(dir.toString());
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
        Configs.resetConfigDirForTest();
    }

    @Test
    void sendsJsonModeRequestAndParsesResponse() {
        DeepSeekClient client = new DeepSeekClient();
        DeepSeekChatResponse response = client.chat(List.of(DeepSeekMessage.user("hi")), "hbot-test", null);
        assertEquals("{\"type\":\"silent\"}", response.getContent());
        assertTrue(chatBody.get().contains("\"response_format\""));
        assertTrue(chatBody.get().contains("\"json_object\""));
        assertTrue(chatBody.get().contains("\"user_id\":\"hbot-test\""));
        assertTrue(chatBody.get().contains("\"thinking\""));
    }

    @Test
    void parsesBalance() {
        DeepSeekClient client = new DeepSeekClient();
        DeepSeekBalanceResponse balance = client.getBalance();
        assertTrue(balance.isAvailable());
        assertEquals("CNY", balance.getBalances().get(0).getCurrency());
        assertEquals("10.00", balance.getBalances().get(0).getTotalBalance());
    }

    private void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }
}
