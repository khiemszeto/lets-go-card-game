package com.gameplatform.service;


import com.gameplatform.game.Room;
import com.gameplatform.game.RoomManager;
import com.gameplatform.websocket.dto.ErrorMessageDto;
import com.gameplatform.websocket.dto.RoomPlayerDto;
import com.gameplatform.websocket.dto.RoomStateMessageDto;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * This class should handle the lobby of users
 * where user can pick a game, and play in a room together?
 * */

@Service
public class LobbyService {

    private RoomManager roomManager;


    public LobbyService(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    public Object createRoom(Long playerId, String username) {
        UUID roomID = roomManager.createRoom(playerId, username);

        if (roomID == null) {
           ErrorMessageDto error =  new ErrorMessageDto();
           error.setType("ERROR");
           error.setMessage("Cannot create room, already in room");
           return error;
        }

        return buildRoomState(roomManager.getRoom(roomID));
    }

    public Object joinRoom(Long playerId, String username, UUID roomId) {
        RoomManager.JoinStatus status = roomManager.joinRoom(playerId, username, roomId);

        switch (status) {
            case SUCCESS -> {
                return buildRoomState(roomManager.getRoom(roomId));
            }

            case ROOM_NOT_FOUND -> {
                return error("Room not found");
            }

            case ROOM_FULL -> {
                return error("Room is full");
            }

            case ALREADY_IN_ROOM -> {
                return error("Already in room");
            }

            default -> throw new IllegalStateException("Unexpected value: " + status);
        }

    }

    private RoomStateMessageDto buildRoomState(Room room) {
        RoomStateMessageDto roomStateMessageDto = new RoomStateMessageDto();

        roomStateMessageDto.setRoomId(room.getId().toString());
        roomStateMessageDto.setReadyPlayers(room.getWhoIsReady().size());

        List<RoomPlayerDto> players = new ArrayList<>();

        for (Map.Entry<Long, String> entry : room.getMapOfPlayers().entrySet()) {
            RoomPlayerDto playerDto = new RoomPlayerDto();

            playerDto.setPlayerId(entry.getKey());
            playerDto.setUsername(entry.getValue());
            playerDto.setReady(room.getWhoIsReady().contains(entry.getKey()));

            players.add(playerDto);
        }

        roomStateMessageDto.setPlayers(players);

        return roomStateMessageDto;
    }

    private ErrorMessageDto error(String message) {
        ErrorMessageDto dto = new ErrorMessageDto();
        dto.setType("ERROR");
        dto.setMessage(message);
        return dto;
    }

}
