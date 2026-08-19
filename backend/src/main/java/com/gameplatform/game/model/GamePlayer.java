package com.gameplatform.game.model;

import java.util.List;

public class GamePlayer {

    private List<Card> hand;



    GamePlayer() {

    }



    public void receiveCards(List<Card> cards) {
        this.hand = cards;
    }

    public boolean playCard(List<Card> decidedCards) {
        // call a method to send our decided Cards to the server
        // where server will validate our decided cards
        // if our decided cards is valid, we remove decided cards from hand
        // else cannot play, hand remains the same, player must decide on something else

        // return if the decided cards successes or not
        return true;
    }



}
