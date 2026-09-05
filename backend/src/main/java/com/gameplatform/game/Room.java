package com.gameplatform.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.gameplatform.game.model.Card;

import com.gameplatform.game.model.GameMove;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Room {
    private static final int MAX_PLAYERS = 4;
    private static final int MIN_PLAYERS = 2;
    private enum Status {
        WAITING, COUNTING_DOWN ,STARTED
    }
    private Status roomStatus;

    private Integer roomId;
    // map playerId to playerName
    private HashMap<Long, String> mapOfPlayers;

    private Set<Long> whoIsReady;

    // Seat Order by playerId
    private List<Long> seatOrder = new ArrayList<>();

    // map playerId to their current card state in a game
    private Map<Long, List<Card>> hands = new HashMap<>();

    private Long currentTurnPlayerId;

    // map left during game playerId to their card state when they left for penalty
    private Map<Long, List<Card>> leftPlayersHands = new HashMap<>();

    // last played card in a trick
    private List<Card> lastPlayCards;

    // the player who played the last card
    private Long lastPlayPlayerId;

    // count how many consecutive passes have been made to determine if the trick is over (>= 3)
    private int consecutivePasses;

    private boolean firstTrickPlayed;


    // *for each game data preservation*
    private Map<Long, List<Card>> startingHands = new HashMap<>();
    private List<Long> startingPlayOrder = new ArrayList<>();
    private Long historyFirstPlayerId;
    private List<GameMove> moveLog = new ArrayList<>();


    // Constructor
    public Room(Integer roomId) {
        this.roomId = roomId;
        mapOfPlayers = new HashMap<>();
        whoIsReady = new HashSet<>();
        roomStatus = Status.WAITING;
        lastPlayCards = null;
        consecutivePasses = 0;
        firstTrickPlayed = false;
    }

    public boolean addPlayer(Long playerId, String playerName) {
        if ((roomStatus == Status.WAITING || roomStatus == Status.COUNTING_DOWN)
                && mapOfPlayers.size() < MAX_PLAYERS
                && !mapOfPlayers.containsKey(playerId)) {
            mapOfPlayers.put(playerId, playerName);
            seatOrder.add(playerId);
            return true;
        }

        return false;
    }

    public void removePlayer(Long playerId) {
        if (mapOfPlayers.containsKey(playerId)) {
            if (whoIsReady.contains(playerId)) {
                whoIsReady.remove(playerId);
            }
            mapOfPlayers.remove(playerId);
            seatOrder.remove(playerId);
        }

    }

    public void setReady(Long playerId) {
        if (!whoIsReady.contains(playerId) && mapOfPlayers.containsKey(playerId)) whoIsReady.add(playerId);
    }

    public void setNotReady(Long playerId) {
        if (whoIsReady.contains(playerId)) whoIsReady.remove(playerId);
    }

    public boolean isReadyToStart() {
        if (whoIsReady.size() >= MIN_PLAYERS) return true;
        return false;
    }

    public boolean isFull() {
        if (mapOfPlayers.size() < MAX_PLAYERS) return false;

        return true;
    }

    public boolean isEmpty() {
        if (mapOfPlayers.isEmpty()) return true;
        return false;
    }

    public boolean isWaiting() {
        return roomStatus == Status.WAITING;
    }

    public boolean isCountingDown() {
        return roomStatus == Status.COUNTING_DOWN;
    }

    public boolean isStarted() {
        return roomStatus == Status.STARTED;
    }

    public boolean startCountingDown() {
        if (roomStatus != Status.WAITING) return false;
        roomStatus = Status.COUNTING_DOWN;
        return true;
    }

    public void cancelCountingDown() {
        if (roomStatus == Status.COUNTING_DOWN) roomStatus = Status.WAITING;
    }

    public void markStarted() {
        roomStatus = Status.STARTED;
    }

    public void setHand(Long playerId, List<Card> cards) {
        hands.put(playerId, cards);
    }

    public List<Card> getHand(Long playerId) {
        return hands.get(playerId);
    }

    public void clearHands() {
        hands.clear();
    }

    public Long nextPlayerId() {
        if (currentTurnPlayerId == null || seatOrder.isEmpty()) return null;

        int start = seatOrder.indexOf(currentTurnPlayerId);

        for (int i = 1; i <= seatOrder.size(); i++) {
            Long candidate = seatOrder.get((start + i) % seatOrder.size());
            List<Card> cards = hands.get(candidate);

            if (cards != null && !cards.isEmpty()) return candidate;
        }
        return null; // nobody left with cards
    }

    public void resetToWaiting() {
        if (roomStatus == Status.STARTED) {
            roomStatus = Status.WAITING;
            whoIsReady.clear();
            hands.clear();
            currentTurnPlayerId = null;
            lastPlayCards = null;
            lastPlayPlayerId = null;
            consecutivePasses = 0;
            firstTrickPlayed = false;
            leftPlayersHands.clear();
            clearHistoryBuffer();
        }
    }

    public void archiveHandForScoring(Long playerId) {
        List<Card> lefthands = hands.remove(playerId);
        if (lefthands != null && !lefthands.isEmpty() ) {
            leftPlayersHands.put(playerId, lefthands);
        }
    }

    // get Hand of all players in the end of the game, including those who left for penalty
    public Map<Long, List<Card>> getAllHandsForScoring() {
        Map<Long, List<Card>> allRemainingHands = new HashMap<>();

        allRemainingHands.putAll(leftPlayersHands);
        allRemainingHands.putAll(hands);

        return allRemainingHands;
    }


    // *data preservation helper*
    public void snapshotStartingHands() {
        startingHands.clear();
        for (Map.Entry<Long, List<Card>> entry : hands.entrySet()) {
            startingHands.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
   }

   public void snapshotSeatsForHistory() {
        startingPlayOrder = new ArrayList<>(seatOrder);
   }

   public void appendMove(GameMove move) {
        moveLog.add(move);
   }

   public void clearHistoryBuffer() {
        startingHands.clear();
        startingPlayOrder.clear();
        moveLog.clear();
        historyFirstPlayerId   = null;
   }




}
