package com.gameplatform.game.rules;

public final class MoveValidator {



    public boolean validateShape() {

        return true;
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

    }
}
