package com.gameplatform.e2e;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

@Order(1)
class SmokeE2ETests extends BaseE2E {

    @Test
    void registerLoginAndReachLobby() {
        registerAndLogin(newBrowser(), "s");
    }
}
