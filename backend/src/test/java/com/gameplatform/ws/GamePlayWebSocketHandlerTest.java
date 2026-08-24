package com.gameplatform.ws;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gameplatform.service.PlayerService;
import com.gameplatform.websocket.GamePlayWebSocketHandler;
import com.gameplatform.websocket.SessionRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

public class GamePlayWebSocketHandlerTest {
    private final PlayerService playerService = mock(PlayerService.class);
    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private ObjectMapper objectMapper = new ObjectMapper();
    private final GamePlayWebSocketHandler handler = new GamePlayWebSocketHandler(sessionRegistry, playerService, objectMapper);

    @Test
    void connectionSendsWelcomeAndOnlineCount() throws Exception {
        WebSocketSession session = openSession("s1");

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(new TextMessage("welcome s1"));
        verify(session).sendMessage(new TextMessage("online 1"));
    }


    @Test
    void messageIsBroadcastToEveryConnectedSession() throws Exception {
        WebSocketSession sender = openSession("s1");
        WebSocketSession other = openSession("s2");
        handler.afterConnectionEstablished(sender);
        handler.afterConnectionEstablished(other);

        handler.handleTextMessage(sender, new TextMessage("hello"));

        verify(sender).sendMessage(new TextMessage("s1: hello"));
        verify(other).sendMessage(new TextMessage("s1: hello"));
    }

    @Test
    void closingRemovesTheSession() throws Exception {
        WebSocketSession session = openSession("s1");
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessionRegistry.count()).isZero();
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }



}
