package com.gameplatform.game.rules;

import java.util.List;
import java.util.Optional;

import com.gameplatform.game.model.Card;

public final class MoveValidator {


    /** Identify the type of play from the List<Cards> from the player
     * return the identified type of play*/
    public Play identifyPlay(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return null;




        return null;
    }


    public boolean validateHand() {

        return true;
    }


    // if A cards outrank B cards
    static boolean outRanks() {
        return true;
    }

    static boolean isBomb() {
        return false;
    }

    static int countPairs() {
        return 0;
    }
}
