package com.gameplatform.e2e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import com.gameplatform.e2e.pages.GamePage;
import com.gameplatform.e2e.pages.RoomPage;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

// Needs APP_DECK_SHUFFLE=false.
// Fixed deal: one player gets all spades, others all clubs / diamonds / hearts.
// Spades player plays 3..A, the other three pass, then 2 of spades wins.
@Order(4)
class FourPlayerFullGameE2ETests extends BaseE2E {

    @Test
    void fourPlayersLeaderPlaysOthersPassAndWins() {
        var p1Driver = newBrowser();
        var p2Driver = newBrowser();
        var p3Driver = newBrowser();
        var p4Driver = newBrowser();

        String p1Name = "qa" + System.currentTimeMillis() + "w";
        String p2Name = "qa" + System.currentTimeMillis() + "x";
        String p3Name = "qa" + System.currentTimeMillis() + "y";
        String p4Name = "qa" + System.currentTimeMillis() + "z";

        // Join all four before anyone readies (countdown starts at 2 ready)
        RoomPage room1 = registerAndLoginAs(p1Driver, p1Name).createRoom();
        String roomId = room1.roomId();
        RoomPage room2 = registerAndLoginAs(p2Driver, p2Name).joinRoom(roomId);
        RoomPage room3 = registerAndLoginAs(p3Driver, p3Name).joinRoom(roomId);
        RoomPage room4 = registerAndLoginAs(p4Driver, p4Name).joinRoom(roomId);

        room1.ready();
        room2.ready();
        room3.ready();
        room4.ready();

        GamePage g1 = new GamePage(p1Driver).waitForHandSize(13);
        GamePage g2 = new GamePage(p2Driver).waitForHandSize(13);
        GamePage g3 = new GamePage(p3Driver).waitForHandSize(13);
        GamePage g4 = new GamePage(p4Driver).waitForHandSize(13);

        List<NamedPlayer> players = List.of(
                new NamedPlayer(p1Name, g1),
                new NamedPlayer(p2Name, g2),
                new NamedPlayer(p3Name, g3),
                new NamedPlayer(p4Name, g4));

        NamedPlayer leader = players.stream()
                .filter(p -> p.game.handCardIds().contains("QA:card-THREE-SPADES"))
                .findFirst()
                .orElseThrow(() -> new AssertionError("No player received THREE-SPADES"));

        List<GamePage> others = players.stream()
                .filter(p -> p != leader)
                .map(p -> p.game)
                .toList();

        assertThat(leader.game.handCardIds()).allMatch(id -> id.endsWith("-SPADES"));
        for (GamePage other : others) {
            assertThat(other.handCardIds()).noneMatch(id -> id.endsWith("-SPADES"));
        }

        leader.game.playCards(
                "THREE-SPADES", "FOUR-SPADES", "FIVE-SPADES", "SIX-SPADES", "SEVEN-SPADES",
                "EIGHT-SPADES", "NINE-SPADES", "TEN-SPADES", "JACK-SPADES", "QUEEN-SPADES",
                "KING-SPADES", "ACE-SPADES");
        leader.game.waitForHandSize(1);

        others.getFirst().waitForLastPlay("ACE", "SPADES");
        letOthersPassInTurnOrder(others, 3);

        leader.game.waitForMyTurn().playCards("TWO-SPADES");

        for (NamedPlayer p : players) {
            assertThat(p.game.winner()).contains(leader.name);
        }
    }

    /** Each of the next {@code passesNeeded} turns among {@code others} clicks Pass. */
    private void letOthersPassInTurnOrder(List<GamePage> others, int passesNeeded) {
        List<GamePage> remaining = new ArrayList<>(others);
        for (int i = 0; i < passesNeeded; i++) {
            GamePage toPass = waitForAnyPassEnabled(remaining);
            toPass.pass();
            remaining.remove(toPass);
        }
    }

    private GamePage waitForAnyPassEnabled(List<GamePage> candidates) {
        long deadline = System.currentTimeMillis() + 30_000;
        while (System.currentTimeMillis() < deadline) {
            for (GamePage page : candidates) {
                if (page.isPassButtonEnabled()) {
                    return page;
                }
            }
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError("Interrupted waiting for a pass turn", e);
            }
        }
        throw new AssertionError("Timed out waiting for another player to pass");
    }

    private record NamedPlayer(String name, GamePage game) {}
}
