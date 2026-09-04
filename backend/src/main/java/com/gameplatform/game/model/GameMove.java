package com.gameplatform.game.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GameMove {
    private String type;
    private Long playerId;
    private List<Card> cards;
}
