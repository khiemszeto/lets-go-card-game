package com.gameplatform.game;

import java.util.ArrayList;
import java.util.List;

import com.gameplatform.entity.Player;
import com.gameplatform.game.model.Card;
import com.gameplatform.game.model.Deck;
import com.gameplatform.game.model.GamePlayer;

public class GameController {

    private int readyPlayerCount;
    private boolean isFinished;
    private Deck deck;

    // TODO: decide where to put and call Validator
    //private Validator validator;

    // TODO: create GamePlayer class
    private List<GamePlayer> gamePlayers;


    public GameController() {
        deck = new Deck();
        isFinished = false;
        readyPlayerCount = 0;
        gamePlayers = new ArrayList<>();
    }

    public void gameStart() {
        while (!isFinished) {
            // deal deck of cards

            deck.shuffle();

            // assign set of cards to the individual players
            for (GamePlayer gamePlayer : gamePlayers) {
                gamePlayer.receiveCards(deck.dealCards());
            }


            // TODO:pass to a method to change the isFinish state?
            // if game updated isFinished to True, then?

        }
    }


    public boolean canWeStart() {

        return true;
    }





    public void joinGame(GamePlayer gamePlayer) {
        // add player to the current list of game players
        if (gamePlayers.size() < 4) gamePlayers.add(gamePlayer);
    }



}
