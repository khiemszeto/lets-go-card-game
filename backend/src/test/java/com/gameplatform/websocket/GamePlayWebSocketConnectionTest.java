package com.gameplatform.websocket;


import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.net.URI;
import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.gameplatform.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.gameplatform.TestcontainersConfiguration;
import com.gameplatform.dto.CreatePlayerRequestDto;
import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.service.AuthPlayerService;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class GamePlayWebSocketConnectionTest {

    @LocalServerPort
    private int port;

    // ask Spring to wire us up to the PlayerService bean such that we can add Player Record to the test DB
    @Autowired
    private AuthPlayerService authPlayerService;

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtService jwtService;

    private record TestPlayer(String username, String password) {}

    @Test
    void clientConnectsAndIsAnsweredOverTheSocket() throws Exception {
        BlockingQueue<String> aliceMessages = new LinkedBlockingQueue<>();
        BlockingQueue<String> bobMessages = new LinkedBlockingQueue<>();

        TestPlayer alice = registerPlayer();
        WebSocketSession aliceSession = connect(alice, aliceMessages);
        assertThat(bobMessages.poll(5, SECONDS)).startsWith("welcome ");
        assertThat(aliceMessages.poll(5, SECONDS)).isEqualTo("online 1");
        TestPlayer bob = registerPlayer();
        WebSocketSession bobSession = connect(bob, bobMessages);
        assertThat(bobMessages.poll(5, SECONDS)).startsWith("welcome ");
        assertThat(bobMessages.poll(5, SECONDS)).isEqualTo("online 2");


        // check if alice received the auto broadcast when new player has joined
        assertThat(aliceMessages.poll(5, SECONDS)).isEqualTo("online 2");

        bobSession.sendMessage(new TextMessage("{\"type\":\"PING\"}"));
        assertThat(bobMessages.poll(5, SECONDS)).isEqualTo("{\"type\":\"PONG\"}");

        aliceSession.close();
        bobSession.close();
    }

    // Open a client side socket, with anonymous subclass of TextWebSocketHandler inline
    private WebSocketSession connect(TestPlayer player, BlockingQueue<String> received) throws Exception {
        String token = tokenFor(player);

        WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
        headers.add(HttpHeaders.COOKIE, "ACCESS_TOKEN=" + token);

        return new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                        received.add(message.getPayload());
                    }
                },
                        headers,
                        URI.create("ws://localhost:" + port + "/ws"))
                .get(5, SECONDS);
    }

    private TestPlayer registerPlayer() {
        String unique = String.valueOf(System.nanoTime());
        String username = "player" + unique;
        String password = "password" + unique; // min 6 chars — satisfies validation
        CreatePlayerRequestDto request = new CreatePlayerRequestDto();
        request.setUsername(username);
        request.setPassword(password);
        request.setEmail(username + "@mailinator.com");
        authPlayerService.createPlayer(request);
        return new TestPlayer(username, password);
    }

    private String tokenFor(TestPlayer player) {
        Authentication auth = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(
                        player.username(), player.password()));
        return jwtService.generateToken(auth);
    }
}
