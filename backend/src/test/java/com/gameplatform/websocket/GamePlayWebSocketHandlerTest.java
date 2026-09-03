package com.gameplatform.websocket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.HashMap;

import com.gameplatform.service.GameService;
import com.gameplatform.service.LobbyService;
import com.gameplatform.service.AuthPlayerService;
import com.gameplatform.websocket.dto.common.ErrorMessageDto;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.ObjectMapper;

public class GamePlayWebSocketHandlerTest {

    private final SessionRegistry sessionRegistry = new SessionRegistry();
    private ObjectMapper objectMapper = new ObjectMapper();
    private final LobbyService lobbyService = mock(LobbyService.class);
    private final GameService gameService = mock(GameService.class);
    private final RoomNotifier roomNotifier = mock(RoomNotifier.class);
    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    // the handler still takes AuthPlayerService but no longer uses it, so a bare mock will do
    private final GamePlayWebSocketHandler handler = new GamePlayWebSocketHandler(
            sessionRegistry, mock(AuthPlayerService.class), lobbyService, gameService, objectMapper, roomNotifier, jwtDecoder);

    @Test
    void connectionSendsWelcomeAndOnlineCount() throws Exception {
        WebSocketSession session = openSession("s1", 1L);

        handler.afterConnectionEstablished(session);

        verify(session).sendMessage(new TextMessage("welcome s1"));
        verify(session).sendMessage(new TextMessage("online 1"));
    }

    @Test
    void badTokenIsRejected() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn("s1");
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=nope"));
        when(jwtDecoder.decode("nope")).thenThrow(new BadJwtException("bad signature"));

        handler.afterConnectionEstablished(session);

        verify(session).close(CloseStatus.BAD_DATA.withReason("Invalid token"));
        assertThat(sessionRegistry.count()).isZero();
    }

    @Test
    void pingIsAnsweredWithPong() throws Exception {
        WebSocketSession session = openSession("s1", 1L);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("{\"type\":\"PING\"}"));

        verify(session).sendMessage(new TextMessage("{\"type\":\"PONG\"}"));
    }

    @Test
    void invalidJsonIsAnsweredWithError() throws Exception {
        WebSocketSession session = openSession("s1", 1L);
        handler.afterConnectionEstablished(session);

        handler.handleTextMessage(session, new TextMessage("hello"));

        ErrorMessageDto expected = new ErrorMessageDto();
        expected.setMessage("Json invalid bro !!");
        verify(session).sendMessage(new TextMessage(objectMapper.writeValueAsString(expected)));
    }

    @Test
    void closingRemovesTheSession() throws Exception {
        WebSocketSession session = openSession("s1", 1L);
        handler.afterConnectionEstablished(session);

        handler.afterConnectionClosed(session, CloseStatus.NORMAL);

        assertThat(sessionRegistry.count()).isZero();
    }

    private WebSocketSession openSession(String id, Long playerId) {
        String token = "token-" + playerId;

        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getId()).thenReturn(id);
        when(session.isOpen()).thenReturn(true);
        when(session.getUri()).thenReturn(URI.create("ws://localhost/ws?token=" + token));
        when(session.getAttributes()).thenReturn(new HashMap<>());

        // the handler reads the player off the token instead of a query param now
        Jwt jwt = Jwt.withTokenValue(token)
                .header("alg", "HS256")
                .subject("player" + playerId)
                .claim("playerId", playerId)
                .build();
        when(jwtDecoder.decode(token)).thenReturn(jwt);

        return session;
    }

}
