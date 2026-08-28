package com.gameplatform.game.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@SuppressWarnings("unused")
@AllArgsConstructor
public class Combo {
    private final ComboType type;
    private final List<Card> cards;
    private final Rank highRank;
    private final Suit highSuit;
}
