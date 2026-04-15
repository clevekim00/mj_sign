package com.mj.sign;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final SignWebSocketHandler signWebSocketHandler;
    private final String[] allowedOriginPatterns;

    public WebSocketConfig(
            SignWebSocketHandler signWebSocketHandler,
            @Value("${sign.websocket.allowed-origin-patterns:http://localhost:*,http://127.0.0.1:*}") String[] allowedOriginPatterns
    ) {
        this.signWebSocketHandler = signWebSocketHandler;
        this.allowedOriginPatterns = allowedOriginPatterns;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(signWebSocketHandler, "/ws/sign")
                .setAllowedOriginPatterns(allowedOriginPatterns);
    }
}
