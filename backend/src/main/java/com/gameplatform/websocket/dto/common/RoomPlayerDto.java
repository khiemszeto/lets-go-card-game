package com.gameplatform.websocket.dto.common;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomPlayerDto {
    private Long playerId;
    private String username;
    private int numberOfCards;
    private boolean ready;
    private int seat;
}
