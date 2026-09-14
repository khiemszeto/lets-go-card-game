package com.gameplatform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.gameplatform.e2e.pages.GamePage;
import com.gameplatform.e2e.pages.RoomPage;
import org.junit.jupiter.api.Test;

// Needs APP_DECK_SHUFFLE=false. One player gets all spades, the other all clubs. 3 of spades leads.
class GameE2ETests extends BaseE2E {

    @Test
    void fixedDealAndFirstPlayReachBothPlayers() {
        var aliceDriver = newBrowser();
        var bobDriver = newBrowser();

        RoomPage aliceRoom = registerAndLogin(aliceDriver, "a").createRoom();
        RoomPage bobRoom = registerAndLogin(bobDriver, "b").joinRoom(aliceRoom.roomId());

        aliceRoom.ready();
        bobRoom.ready();

        GamePage alice = new GamePage(aliceDriver).waitForHandSize(13);
        GamePage bob = new GamePage(bobDriver).waitForHandSize(13);

        boolean aliceLeads = alice.handCardIds().contains("QA:card-THREE-SPADES");
        GamePage leader = aliceLeads ? alice : bob;
        GamePage other = aliceLeads ? bob : alice;

        assertThat(leader.handCardIds()).allMatch(id -> id.endsWith("-SPADES"));
        assertThat(other.handCardIds()).allMatch(id -> id.endsWith("-CLUBS"));

        leader.play("THREE", "SPADES");

        leader.waitForLastPlay("THREE", "SPADES");
        other.waitForLastPlay("THREE", "SPADES");
        leader.waitForHandSize(12);
    }
}
