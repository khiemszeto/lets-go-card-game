package com.gameplatform.websocket.dto.outbound.game;

import com.gameplatform.websocket.dto.common.CardDto;
import com.gameplatform.websocket.dto.common.RoomPlayerDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class PlayResultMessageDto
{
    private final String type = "PLAY_RESULT";
    private String roomId;
    private Long currentPlayerId;
    private Long playerId;
    private List<RoomPlayerDto> players;
    private boolean trickReset;
    private List<CardDto> cardsPlayed;
}
