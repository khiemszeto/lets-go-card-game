package com.gameplatform.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.gameplatform.game.model.Card;

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

    private UUID id;
    private HashMap<Long, String> mapOfPlayers; // playerId -> playerName *
    private Set<Long> whoIsReady;
    private List<Long> seatOrder = new ArrayList<>(); // seat order *
    private Map<Long, List<Card>> hands = new HashMap<>(); // playerId -> their cards
    private Long currentTurnPlayerId; // current player to play
    private Map<Long, List<Card>> leftPlayersHands = new HashMap<>();

    private List<Card> lastPlayCards;
    private Long lastPlayPlayerId;
    private int consecutivePasses;
    private boolean firstTrickPlayed;


    public Room() {
        id = UUID.randomUUID();
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
        }
    }

    public void archiveHandForScoring(Long playerId) {
        List<Card> lefthands = hands.remove(playerId);
        if (lefthands != null && !lefthands.isEmpty() ) {
            leftPlayersHands.put(playerId, lefthands);
        }
    }

    public Map<Long, List<Card>> getAllHandsForScoring() {
        Map<Long, List<Card>> allRemainingHands = new HashMap<>();

        allRemainingHands.putAll(leftPlayersHands);
        allRemainingHands.putAll(hands);

        return allRemainingHands;
    }






}
