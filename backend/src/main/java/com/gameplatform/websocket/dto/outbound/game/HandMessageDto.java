package com.gameplatform.websocket.dto.outbound.game;

import com.gameplatform.websocket.dto.common.CardDto;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
@Getter
@Setter
@NoArgsConstructor
public class HandMessageDto {
    private final String type = "HAND";
    private String roomId;
    private List<CardDto> cards;
    private boolean ready;
}
