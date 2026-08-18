package com.gameplatform.ws;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

public class GamePlayWebSocketHandlerTest {


    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private final GamePlayWebSocketHandler handler = new GamePlayWebSocketHandler(sessionRegistry);

    @Test
    void connectionSendsWelcomeAndOnlineCount() throws Exception {

    }


    @Test
    void messageIsBroadcastToEveryConnectedSession() throws Exception {


    }

    @Test
    void closingRemovesTheSession() throws Exception {
    }

    private WebSocketSession openSession(String id) {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        return session;
    }



}
