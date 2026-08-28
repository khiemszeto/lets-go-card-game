package com.gameplatform.websocket.dto.outbound.lobby;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LeftRoomMessageDto {
    private String type;
    private String roomId;
}
