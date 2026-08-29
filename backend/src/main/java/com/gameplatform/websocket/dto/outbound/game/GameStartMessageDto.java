package com.gameplatform.websocket.dto.outbound.game;


import com.gameplatform.websocket.dto.common.RoomPlayerDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class GameStartMessageDto {
    private final String type = "GAME_START";
    private String roomId;
    private List<RoomPlayerDto> players;
    private Long currentPlayerId;
}
