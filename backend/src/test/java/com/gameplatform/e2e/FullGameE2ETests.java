package com.gameplatform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import com.gameplatform.e2e.pages.GamePage;
import com.gameplatform.e2e.pages.RoomPage;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

// Needs APP_DECK_SHUFFLE=false
// Spades player plays 3..A as a straight, clubs player passes, 2 of spades wins.
@Order(3)
class FullGameE2ETests extends BaseE2E {

    @Test
    void leaderPlaysOutHandAndWins() {
        var aliceDriver = newBrowser();
        var bobDriver = newBrowser();

        String aliceName = "qa" + System.currentTimeMillis() + "a";
        String bobName = "qa" + System.currentTimeMillis() + "b";

        RoomPage aliceRoom = registerAndLoginAs(aliceDriver, aliceName).createRoom();
        RoomPage bobRoom = registerAndLoginAs(bobDriver, bobName).joinRoom(aliceRoom.roomId());

        aliceRoom.ready();
        bobRoom.ready();

        GamePage alice = new GamePage(aliceDriver).waitForHandSize(13);
        GamePage bob = new GamePage(bobDriver).waitForHandSize(13);

        boolean aliceLeads = alice.handCardIds().contains("QA:card-THREE-SPADES");
        GamePage leader = aliceLeads ? alice : bob;
        GamePage other = aliceLeads ? bob : alice;
        String leaderName = aliceLeads ? aliceName : bobName;

        leader.playCards("THREE-SPADES", "FOUR-SPADES", "FIVE-SPADES", "SIX-SPADES", "SEVEN-SPADES",
                "EIGHT-SPADES", "NINE-SPADES", "TEN-SPADES", "JACK-SPADES", "QUEEN-SPADES",
                "KING-SPADES", "ACE-SPADES");
        // Hand update also clears the selection
        leader.waitForHandSize(1);

        other.waitForLastPlay("ACE", "SPADES");
        other.pass();

        // Pass result clears the selection, so wait for it before selecting
        leader.waitForMyTurn().playCards("TWO-SPADES");

        // Banner only shows for 5s
        assertThat(leader.winner()).contains(leaderName);
        assertThat(other.winner()).contains(leaderName);
    }
}
