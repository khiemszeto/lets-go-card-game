package com.gameplatform.websocket.dto.common;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class CardDto {
    private String suit;
    private String rank;
}
