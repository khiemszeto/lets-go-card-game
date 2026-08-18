package com.gameplatform.ws;


import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import com.gameplatform.TestcontainersConfiguration;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Import(TestcontainersConfiguration.class)
public class GamePlayWebSocketConnectionTest {

    @LocalServerPort
    private int port;

    @Test
    void clientConnectsAndIsEchoed() throws Exception {


    }

}
