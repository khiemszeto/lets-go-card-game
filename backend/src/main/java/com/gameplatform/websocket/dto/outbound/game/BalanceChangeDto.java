package com.gameplatform.websocket.dto.outbound.game;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BalanceChangeDto {
    private String playerName;
    private Long delta;
    private Long newBalance;
}
