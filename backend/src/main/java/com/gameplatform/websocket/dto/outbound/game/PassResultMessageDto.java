package com.gameplatform.websocket.dto.outbound.game;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class PassResultMessageDto {
    private final String type ="PASS_RESULT";
    private String roomId;
    private Long currentPlayerId;
    private Long playerId;
    private boolean trickReset;
}
