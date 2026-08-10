package com.gameplatform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


public class Deck {
    public final List<Card> cards = new ArrayList<>();
    public int remainingStart = 0;
    public int dealSize = 13;

    Deck() {
        for (Suit suit : Suit.values()) {
            for (Rank rank : Rank.values()) {
                cards.add(new Card(suit, rank));
//                System.out.println(cards.getLast().toString());
            }
        }
    }

    public void shuffle() {
        Collections.shuffle(cards);
        System.out.println(cards);
    }

    public List<Card> dealCards() {
        if (cards.isEmpty()) throw new IllegalStateException("No cards in Deck");

        List<Card> dealCards = new ArrayList<>();

        for (int i = 0; i <= 12; i++) {
            dealCards.add(cards.removeLast());
        }
        System.out.println(dealCards);
        return  dealCards;
    }


}
