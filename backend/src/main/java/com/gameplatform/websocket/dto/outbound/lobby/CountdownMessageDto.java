package com.gameplatform.websocket.dto.outbound.lobby;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CountdownMessageDto {
    private String type;
    private String roomId;
    private int seconds;

}
