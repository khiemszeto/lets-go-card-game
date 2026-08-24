package com.gameplatform.game;

import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {

    private Map<UUID, Room> rooms ;

    private Map<Long, UUID> playerToRoom ;

    public enum JoinStatus {
        SUCCESS,
        ALREADY_IN_ROOM,
        ROOM_NOT_FOUND,
        ROOM_FULL
    }

    public RoomManager() {
        this.rooms = new ConcurrentHashMap<>();
        this.playerToRoom = new ConcurrentHashMap<>();
    }

    public Room getRoom(UUID roomId) {
        return rooms.get(roomId);
    }

    public Room getRoomForPlayer(Long playerId) {
        if (playerToRoom.containsKey(playerId)) {
            UUID roomId = playerToRoom.get(playerId);
            return rooms.get(roomId);
        }
        return null;
    }

    public UUID createRoom(Long playerId, String playerName) {
        if (playerToRoom.containsKey(playerId)) return null;

        Room room = new Room();
        room.addPlayer(playerId, playerName);
        rooms.put(room.getId(), room);
        playerToRoom.put(playerId, room.getId());

        return room.getId();
    }

    public JoinStatus joinRoom(Long playerId, String playerName, UUID roomId) {

        if (playerToRoom.containsKey(playerId)) return JoinStatus.ALREADY_IN_ROOM;
        if (!rooms.containsKey(roomId)) return JoinStatus.ROOM_NOT_FOUND;
        if (rooms.get(roomId).isFull()) return JoinStatus.ROOM_FULL;

        Room room = rooms.get(roomId);
        if (!room.addPlayer(playerId, playerName)) return JoinStatus.ROOM_FULL;
        playerToRoom.put(playerId, roomId);

        return JoinStatus.SUCCESS;

    }


    public void leaveRoom(Long playerId) {
        if (playerToRoom.containsKey(playerId)) {
            UUID roomId = playerToRoom.get(playerId);
            Room room = rooms.get(roomId);
            room.removePlayer(playerId);
            playerToRoom.remove(playerId);

            if (room.isEmpty()) {
                rooms.remove(roomId);

            }
        }
    }




}
