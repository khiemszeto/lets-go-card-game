package com.gameplatform.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * Behavior? during connection
 *
 * Handles the lifecycle of a player's realtime connection.
 *
 * For now this only tracks who are connected and echoes messages back to everyone.
 * Basically to check the end to end connection.
 *
 * Probably will build Game room and Lobby routing on top of this.
 * */
@Component
public class GamePlayWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GamePlayWebSocketHandler.class);

    private final SessionRegistry sessionRegistry;

    public GamePlayWebSocketHandler(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        // update map to remember this new connected section
        sessionRegistry.add(session);
        log.info("WebSocket connected: {} ({} online)", session.getId(), sessionRegistry.count());

        sessionRegistry.send(session, new TextMessage("welcome " + session.getId()));
        sessionRegistry.broadcast("online " + sessionRegistry.count());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("WebSocket message from {}: {}", session.getId(), message.getPayload());

        sessionRegistry.broadcast(session.getId() + ": " + message.getPayload());
    }

    // this does not handle the case where laptop lid or a dropped Wi-Fi
    // where socket may still sit there looking open before we verify it failure
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // remove recorded session from mapping
        sessionRegistry.remove(session);
        log.info("WebSocket disconnected: {} ({} online)", session.getId(), sessionRegistry.count());

        // update everyone about this
        sessionRegistry.broadcast("online " + sessionRegistry.count());
    }
}
