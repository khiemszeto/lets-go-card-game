package com.gameplatform.websocket;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.gameplatform.service.PlayerService;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

public class GamePlayWebSocketHandlerTest {

    private final PlayerService playerService = mock(PlayerService.class);
    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private final GamePlayWebSocketHandler handler = new GamePlayWebSocketHandler(sessionRegistry,
            playerService);

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
