package com.gameplatform.websocket.dto.outbound.game;

import com.gameplatform.websocket.dto.common.RoomPlayerDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class LeftDuringGameMessageDto {
    private final String type = "LEFT_DURING_GAME";
    private String roomId;
    private Long leftPlayerId;
    private String leftPlayerUsername;
    private Long currentPlayerId;
    private List<RoomPlayerDto> players;
    private boolean trickReset;
    private boolean autoPassed; // true if it is a pass, false if it is a skip


}
