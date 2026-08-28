package com.gameplatform.websocket.dto.outbound.lobby;

import com.gameplatform.websocket.dto.common.RoomPlayerDto;
import lombok.Getter;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
public class RoomStateMessageDto {
    private final String type = "ROOM_STATE";
    private String roomId;
    private List<RoomPlayerDto> players;
    private int readyPlayers;
}
