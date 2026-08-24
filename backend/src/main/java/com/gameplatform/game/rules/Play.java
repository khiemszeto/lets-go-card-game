package com.gameplatform.game.rules;

import java.util.List;

import com.gameplatform.game.model.Card;

public record Play(PlayType type, List<Card> cards, Card topCard) {

    public Play {
        cards = List.copyOf(cards);
    }

    public int size() {
        return cards.size();
    }

}
