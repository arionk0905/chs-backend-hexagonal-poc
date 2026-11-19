package com.chs.channel;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("core-http")
@Import(MariaDbTestConfiguration.class)
class CoreHttpProfileTest {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final AtomicReference<JsonNode> SENT = new AtomicReference<>();
    private static final HttpServer CORE = startCore();

    @Autowired
    MockMvc mvc;

    @DynamicPropertySource
    static void pointAtStub(DynamicPropertyRegistry properties) {
        properties.add("core.http.base-url", () -> "http://127.0.0.1:" + CORE.getAddress().getPort());
    }

    @AfterAll
    static void stopCore() {
        CORE.stop(0);
    }

    @Test
    void switchesToHttpAndMapsTheSameChannelResponse() throws Exception {
        mvc.perform(post("/api/v1/core/product-inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"SAMPLE-DEPOSIT\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productName").value("HTTP 계정계 상품"))
                .andExpect(jsonPath("$.header").doesNotExist());

        JsonNode request = SENT.get();
        assertEquals("CHS-CORE-001", request.at("/header/messageId").asString());
        assertEquals("1.0", request.at("/header/version").asString());
        assertDoesNotThrow(() -> UUID.fromString(request.at("/header/requestId").asString()));
        assertEquals("SAMPLE-DEPOSIT", request.at("/body/productCode").asString());
    }

    @Test
    void mapsMismatchedResponseToBadGateway() throws Exception {
        mvc.perform(post("/api/v1/core/product-inquiries")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productCode\":\"MISMATCH\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502));
    }

    private static HttpServer startCore() {
        try {
            var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/core/v1/product-inquiries", exchange -> {
                try (exchange) {
                    if (!"POST".equals(exchange.getRequestMethod())) {
                        exchange.sendResponseHeaders(405, -1);
                        return;
                    }
                    JsonNode request = JSON.readTree(exchange.getRequestBody());
                    String code = request.at("/body/productCode").asString();
                    var header = request.path("header");
                    if ("MISMATCH".equals(code)) {
                        header = JSON.createObjectNode().put("requestId", UUID.randomUUID().toString());
                    } else {
                        SENT.set(request);
                    }
                    byte[] body = JSON.writeValueAsBytes(Map.of(
                            "header", header,
                            "resultCode", "0000",
                            "body", Map.of("productCode", code, "productName", "HTTP 계정계 상품")));
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, body.length);
                    exchange.getResponseBody().write(body);
                }
            });
            server.start();
            return server;
        } catch (IOException error) {
            throw new IllegalStateException("계정계 테스트 서버를 시작할 수 없습니다.", error);
        }
    }
}
