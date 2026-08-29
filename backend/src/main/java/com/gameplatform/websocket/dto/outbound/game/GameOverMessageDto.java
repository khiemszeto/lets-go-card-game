package com.gameplatform.websocket.dto.outbound.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class GameOverMessageDto {
    private final String type = "GAME_OVER";
    private String roomId;
    private Long winnerId;

}
