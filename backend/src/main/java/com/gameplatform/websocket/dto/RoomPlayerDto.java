package com.gameplatform.websocket.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomPlayerDto {
    private Long playerId;
    private String username;
    private boolean ready;
}
