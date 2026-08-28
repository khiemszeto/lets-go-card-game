package com.gameplatform.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RoomSummaryDto {
    private String roomId;
    private String hostUsername;
    private int playerCount;
    private int maxPlayers;
    private int readyPlayers;
    private String status;
    private boolean joinable;
}
