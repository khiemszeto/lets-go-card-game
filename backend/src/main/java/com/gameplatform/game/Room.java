package com.gameplatform.game;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class Room {
    private static final int MAX_PLAYERS = 4;
    private static final int MIN_PLAYERS = 2;

    private UUID id;
    private HashMap<Long, String> mapOfPlayers;
    private Set<Long> whoIsReady;



    private enum Status {
        WAITING, STARTED
    }

    private Status roomStatus;

    public Room() {
        id = UUID.randomUUID();
        mapOfPlayers = new HashMap<>();
        whoIsReady = new HashSet<>();
        roomStatus = Status.WAITING;
    }


    public boolean addPlayer(Long playerId, String playerName) {
        if (roomStatus == Status.WAITING
                && mapOfPlayers.size() < MAX_PLAYERS
                && !mapOfPlayers.containsKey(playerId)) {
            mapOfPlayers.put(playerId, playerName);
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
}
