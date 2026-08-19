package com.gameplatform.ws;


import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.gameplatform.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class GamePlayWebSocketConnectionTest {

    @LocalServerPort
    private int port;

    @Test
    void clientConnectsAndIsEchoed() throws Exception {
        BlockingQueue<String> received = new LinkedBlockingQueue<>();

        WebSocketSession session = new StandardWebSocketClient()
                .execute(new TextWebSocketHandler() {
                    @Override
                    protected void handleTextMessage(WebSocketSession s, TextMessage message) {
                        received.add(message.getPayload());
                    }
                }, "ws://localhost:" + port + "/ws")
                .get(5, SECONDS);

        assertThat(received.poll(5, SECONDS)).startsWith("welcome ");
        assertThat(received.poll(5, SECONDS)).isEqualTo("online 1");

        session.sendMessage(new TextMessage("ping"));
        assertThat(received.poll(5, SECONDS)).endsWith(": ping");

        session.close();
    }

}
