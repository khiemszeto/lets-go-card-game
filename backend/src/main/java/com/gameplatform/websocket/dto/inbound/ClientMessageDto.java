package com.gameplatform.websocket.dto.inbound;

import com.gameplatform.websocket.dto.common.CardDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class ClientMessageDto {
    private String type;
    private String roomId;
    private String isReady;
    private List<CardDto> cards;
}
