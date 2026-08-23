package com.gameplatform.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;

import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.exception.ResourceNotFoundException;
import com.gameplatform.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

public class GamePlayWebSocketHandlerTest {

    private final PlayerService playerService = mock(PlayerService.class);
    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private final GamePlayWebSocketHandler handler = new GamePlayWebSocketHandler(sessionRegistry,
            playerService);

    @Test
    void connectionSendsWelcomeAndOnlineCount() throws Exception {
        WebSocketSession session = openSession("s1", 1L);

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(new TextMessage("welcome s1"));
        verify(session).sendMessage(new TextMessage("online 1"));
    }

    @Test
    void unknownPlayerIsRejected() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?playerId=99"));
        when(playerService.getPlayer(99L)).thenThrow(new ResourceNotFoundException("nope"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA.withReason("Invalid playerId"));
        assertThat(sessionRegistry.count()).isZero();
    }

    @Test
    void messageIsBroadcastToEveryConnectedSession() throws Exception {
        WebSocketSession sender = openSession("s1", 1L);
        WebSocketSession other = openSession("s2", 2L);
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(other);

        handler.handleTextMessage(sender, new TextMessage("hello"));

        verify(sender).sendMessage(new TextMessage("s1: hello"));
        verify(other).sendMessage(new TextMessage("s1: hello"));
    }

    @Test
    void closingRemovesTheSession() throws Exception {
        WebSocketSession session = openSession("s1", 1L);
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessionRegistry.count()).isZero();
    }

    private WebSocketSession openSession(String id, Long playerId) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?playerId=" + playerId));
        when(session.getAttributes()).thenReturn(new HashMap<>());

        CreatePlayerResponseDto player = new CreatePlayerResponseDto();
        player.setId(playerId);
        player.setUsername("player" + playerId);
        when(playerService.getPlayer(playerId)).thenReturn(player);

        return session;
    }

}
