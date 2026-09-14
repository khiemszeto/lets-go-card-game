package com.gameplatform.e2e;

import org.junit.jupiter.api.Test;

class SmokeE2ETests extends BaseE2E {

    @Test
    void registerLoginAndReachLobby() {
        registerAndLogin(newBrowser(), "s");
    }
}
