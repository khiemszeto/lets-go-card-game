package com.gameplatform.websocket.dto.outbound.game;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GameOverMessageDto {
    private final String type = "GAME_OVER";
    private String roomId;
    private Long winnerId;
    private List<BalanceChangeDto> balances;

}
