package com.gameplatform.game.model;

import java.util.Comparator;
import java.util.Objects;

public class Card implements Comparable<Card>{

    // this order mechanism rely on the defined order pf enum Rank and Suite,
    // Reordering those enum constants will cause this  Comparator to break
    private static final Comparator<Card> ORDER =
            Comparator.comparing(Card::getRank).thenComparing(Card::getSuit);

    private final Suit suit;
    private final Rank rank;

    public Card(Suit suit, Rank rank) {
        this.suit = suit;
        this.rank = rank;
    }

    public Suit getSuit() {
        return this.suit;
    }

    public Rank getRank() {
        return this.rank;
    }

    @Override
    public int compareTo(Card other) {
        return ORDER.compare(this, other);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true; // same obj
        if (!(o instanceof Card other)) return false; // check if o is instance of Card, if yes, update assignment other = o
        return suit == other.suit && rank == other.rank;
    }

    @Override
    public int hashCode() { // part of the contract of equals(), as equal objects must return equal hashes (Object, not Comparable)
        return Objects.hash(suit, rank);
    }

    public String toString() {
        return rank.toString() + " " + suit.toString();
    }
}
