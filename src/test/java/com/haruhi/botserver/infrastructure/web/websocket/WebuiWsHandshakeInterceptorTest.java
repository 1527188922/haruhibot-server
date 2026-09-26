package com.haruhi.botserver.infrastructure.web.websocket;

import cn.hutool.jwt.JWT;
import com.haruhi.botserver.administration.service.LoginService;
import com.haruhi.botserver.configuration.metadata.ConfigKey;
import com.haruhi.botserver.configuration.service.Configs;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * webui WebSocket 握手鉴权测试：使用真实 LoginService + 自签JWT，不依赖Spring容器
 */
class WebuiWsHandshakeInterceptorTest {

    private static final String JWT_SECRET = "unit-test-jwt-secret";

    @TempDir
    Path configDir;

    private WebuiWsHandshakeInterceptor interceptor;

    @BeforeEach
    void setUp() {
        Configs.useConfigDirForTest(configDir.toString());
        Configs.override(ConfigKey.WEBUI_JWT_SECRET, JWT_SECRET);
        interceptor = new WebuiWsHandshakeInterceptor(new LoginService());
    }

    @AfterEach
    void tearDown() {
        Configs.resetConfigDirForTest();
    }

    @Test
    void acceptsValidTokenAndKeepsUsername() {
        Map<String, Object> attributes = new HashMap<>();
        assertTrue(interceptor.beforeHandshake(request(validToken()), response(), null, attributes));
        assertEquals("admin", attributes.get(WebuiWsHandshakeInterceptor.ATTR_USERNAME));
    }

    @Test
    void rejectsInvalidToken() {
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        Map<String, Object> attributes = new HashMap<>();

        assertFalse(interceptor.beforeHandshake(request("not-a-jwt"), new ServletServerHttpResponse(mockResponse),
                null, attributes));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
        assertNull(attributes.get(WebuiWsHandshakeInterceptor.ATTR_USERNAME));
    }

    @Test
    void rejectsTokenSignedByAnotherSecret() {
        String forged = JWT.create()
                .setPayload(LoginService.JWT_PAYLOAD_USERNAME, "admin")
                .setIssuedAt(new Date())
                .setExpiresAt(new Date(System.currentTimeMillis() + 3600_000L))
                .setKey("another-secret".getBytes(StandardCharsets.UTF_8))
                .sign();
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();

        assertFalse(interceptor.beforeHandshake(request(forged), new ServletServerHttpResponse(mockResponse),
                null, new HashMap<>()));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
    }

    @Test
    void rejectsMissingToken() {
        MockHttpServletResponse mockResponse = new MockHttpServletResponse();
        assertFalse(interceptor.beforeHandshake(request(null), new ServletServerHttpResponse(mockResponse),
                null, new HashMap<>()));
        assertEquals(HttpStatus.UNAUTHORIZED.value(), mockResponse.getStatus());
    }

    private String validToken() {
        long now = System.currentTimeMillis();
        return JWT.create()
                .setPayload(LoginService.JWT_PAYLOAD_USERNAME, "admin")
                .setIssuedAt(new Date(now))
                .setExpiresAt(new Date(now + 3600_000L))
                .setKey(JWT_SECRET.getBytes(StandardCharsets.UTF_8))
                .sign();
    }

    private ServletServerHttpRequest request(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/webui/ws");
        if (token != null) {
            request.setQueryString("token=" + token);
            request.setParameter("token", token);
        }
        return new ServletServerHttpRequest(request);
    }

    private ServletServerHttpResponse response() {
        return new ServletServerHttpResponse(new MockHttpServletResponse());
    }
}
