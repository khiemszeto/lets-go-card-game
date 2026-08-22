package com.gameplatform.websocket;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Wiring?  Fire up once at startup to establish the connection
 *
 * Wires the WebSocket endpoint that browsers connect to.
 *
 * Allowed origins cover the Vite dev server and the packaged app served by Spring.
 *
 * */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final GamePlayWebSocketHandler gamePlayWebSocketHandler;

    public WebSocketConfig(GamePlayWebSocketHandler gamePlayWebSocketHandler) {
        this.gamePlayWebSocketHandler = gamePlayWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(gamePlayWebSocketHandler, "/ws")
                .setAllowedOrigins("http://localhost:5173", "http://localhost:8080");
    }
}
