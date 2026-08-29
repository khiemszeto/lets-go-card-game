package com.gameplatform.websocket;


import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.time.LocalDate;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.gameplatform.TestcontainersConfiguration;
import com.gameplatform.dto.CreatePlayerRequestDto;
import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.service.PlayerService;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class GamePlayWebSocketConnectionTest {

    @LocalServerPort
    private int port;

    // ask Spring to wire us up to the PlayerService bean such that we can add Player Record to the test DB
    @Autowired
    private PlayerService playerService;

    @Test
    void clientConnectsAndIsAnsweredOverTheSocket() throws Exception {
        BlockingQueue<String> alice = new LinkedBlockingQueue<>();
        BlockingQueue<String> bob = new LinkedBlockingQueue<>();

        WebSocketSession aliceSession = connect(createPlayer().getId(), alice);
        assertThat(alice.poll(5, SECONDS)).startsWith("welcome ");
        assertThat(alice.poll(5, SECONDS)).isEqualTo("online 1");

        WebSocketSession bobSession = connect(createPlayer().getId(), bob);
        assertThat(bob.poll(5, SECONDS)).startsWith("welcome ");
        assertThat(bob.poll(5, SECONDS)).isEqualTo("online 2");

        // check if alice received the auto broadcast when new player has joined
        assertThat(alice.poll(5, SECONDS)).isEqualTo("online 2");

        bobSession.sendMessage(new TextMessage("{\"type\":\"PING\"}"));
        assertThat(bob.poll(5, SECONDS)).isEqualTo("{\"type\":\"PONG\"}");

        aliceSession.close();
        bobSession.close();
    }
    // Open a client side socket, with anonymous subclass of TextWebSocketHandler inline
    private WebSocketSession connect(Long playerId, BlockingQueue<String> received) throws Exception {
        return new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    // Override handler to receive server response
                    @Override
                    protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                        received.add(message.getPayload());
                    }
                }, "ws://localhost:" + port + "/ws?playerId=" + playerId)
                .get(5, SECONDS);
    }

    private CreatePlayerResponseDto createPlayer() {
        String unique = String.valueOf(System.nanoTime());

        CreatePlayerRequestDto request = new CreatePlayerRequestDto();
        request.setUsername("player" + unique);
        request.setPassword("password" + unique);
        request.setEmail("player" + unique + "@mailinator.com");
        request.setBirthDate(LocalDate.of(2000, 1, 1));
        return playerService.createPlayer(request);
    }
}
