package com.gameplatform.websocket.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClientMessageDto {
    private String type;
    private String roomId;
}
