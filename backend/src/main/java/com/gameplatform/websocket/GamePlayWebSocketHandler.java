package com.gameplatform.websocket;

import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.exception.ResourceNotFoundException;
import com.gameplatform.service.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URI;

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

    PlayerService playerService;

    public GamePlayWebSocketHandler(SessionRegistry sessionRegistry,
                                    PlayerService playerService) {
        this.sessionRegistry = sessionRegistry;
        this.playerService = playerService;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing URI"));
            return;
        }

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String playerIdString = params.getFirst("playerId");

        if (playerIdString == null || playerIdString.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing playerId"));
            return;
        }

        Long playerId;
        try {
            playerId = Long.parseLong(playerIdString.trim());
        }catch (NumberFormatException e) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid playerId"));
            return;
        }

        CreatePlayerResponseDto player;
        try {
            player = playerService.getPlayer(playerId);
        } catch (ResourceNotFoundException e) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid playerId"));
            return;
        }

        session.getAttributes().put("playerId", playerId);
        session.getAttributes().put("username", player.getUsername());

        // update map to remember this new connected section
        sessionRegistry.add(session, playerId);
        log.info("WebSocket connected: player {} ({}) — {} online",
                player.getUsername(), player.getId(), sessionRegistry.count());
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
