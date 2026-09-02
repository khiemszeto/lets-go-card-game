package com.gameplatform.game;

import org.springframework.stereotype.Component;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;



@Component
public class RoomManager {
    private static final int MAX_ROOMS = 100;
    private Map<Integer, Room> rooms ;
    private Map<Long, Integer> playerToRoom ;

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

    public Room getRoom(Integer roomId) {
        return rooms.get(roomId);
    }

    public Room getRoomForPlayer(Long playerId) {
        if (playerToRoom.containsKey(playerId)) {
            Integer roomId = playerToRoom.get(playerId);
            return rooms.get(roomId);
        }
        return null;
    }

    public Integer createRoom(Long playerId, String playerName) {
        if (playerToRoom.containsKey(playerId)) return null;
        if (rooms.size() >= MAX_ROOMS) return null;

        Integer roomId = findFreeRoomId();
        if (roomId == null) return null;

        Room room = new Room(roomId);
        room.addPlayer(playerId, playerName);
        rooms.put(roomId, room);
        playerToRoom.put(playerId, roomId);

        return roomId;
    }

    public JoinStatus joinRoom(Long playerId, String playerName, Integer roomId) {

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
            Integer roomId = playerToRoom.get(playerId);
            Room room = rooms.get(roomId);
            room.removePlayer(playerId);
            playerToRoom.remove(playerId);

            if (room.isEmpty()) {
                rooms.remove(roomId);
            }
        }
    }

    public java.util.Collection<Room> listRooms() {
        return rooms.values();
    }

    private Integer findFreeRoomId() {
        for (int id = 1; id <= MAX_ROOMS; id++) {
            if (!rooms.containsKey(id)) return id;
        }
        return null; // all 100 slots in use
    }


}
