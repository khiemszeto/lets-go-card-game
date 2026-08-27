package com.gameplatform.game.rules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import com.gameplatform.game.model.Card;
import com.gameplatform.game.model.Rank;

/**
 * Where the game rules live
 * All static, class method only
 * */
public final class MoveValidator {

    private static final int THREE_PAIR_RUN_CARDS = 6; // 3 pairs == 6 cards
    private static final int FOUR_PAIR_RUN_CARDS = 8; // similarly, 4 pairs == 8 cards

    private MoveValidator() {}

    /** the cards must form a legal play AND beat whatever is on the table */
    public static MoveResult validate(List<Card> cards, Table table) {
        MoveResult identified = identifyPlay(cards);
        if (!identified.valid()) return identified; // not even a legal shape, stop here

        // nothing on the table, so any legal shape goes
        if (table == null || table.isEmpty()) return identified;

        if (!beats(identified.play(), table.standing())) {
            return MoveResult.invalid("this does not beat the "
                    + describe(table.standing()) + " on the table");
        }
        return identified;
    }

    /** figure out which PlayType does the List<Card> form into */
    public static MoveResult identifyPlay(List<Card> cards) {
        if (cards == null || cards.isEmpty()) {
            return MoveResult.invalid("player must play at least one card");
        }
        // maybe can skip this if the front end already check the card selection?
        // but some also says that "Never Trust the Client"
        if (new HashSet<>(cards).size() != cards.size()) {
            return MoveResult.invalid("player cannot play the same card twice");
        }
        List<Card> sorted = new ArrayList<>(cards);
        Collections.sort(sorted);
        Card top = sorted.getLast();
        int n = sorted.size();
        // Play single card
        if (n == 1) {
            return MoveResult.ok(new Play(PlayType.SINGLE, sorted, top));
        }
        // if all cards from Play are same rank, figure which type
        if (isAllSameRank(sorted)) {
            if (n == 2) return MoveResult.ok(new Play(PlayType.PAIR, sorted, top));
            if (n == 3) return MoveResult.ok(new Play(PlayType.TRIPLE, sorted, top));
            if (n == 4) return  MoveResult.ok(new Play(PlayType.FOUR_OF_A_KIND, sorted, top));
        }
        // if cards form a Straight
        if (isStraight(sorted)) {
            return MoveResult.ok(new Play(PlayType.STRAIGHT, sorted, top));
        }
        // if form consecutive paris 33-44-55
        if (isConsecutivePairs(sorted)) {
            return MoveResult.ok(new Play(PlayType.CONSECUTIVE_PAIRS, sorted, top));
        }
        return MoveResult.invalid(rejectionReason(n));
    }

    /** loop through card to check if all of them are the same rank */
    private static boolean isAllSameRank(List<Card> sorted) {
        Rank first = sorted.getFirst().getRank();
        for (Card card : sorted) {
            if (card.getRank() != first) return false;
        }
        return true;
    }

    /**
     * have at least 3 or more cards,
     * that are consecutive ranks
     *
     * Ignore no 2
     * Suits can be anything */
    private static boolean isStraight(List<Card> sorted) {
        if (sorted.size() < 3) return false; // must have at least 3 cards
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).getRank() == Rank.TWO) return false; // cannot have no 2
            if (i > 0 && !isOneRankAbove(sorted.get(i), sorted.get(i - 1))) return false; // must be consecutive ranks of cards
        }
        return true;
    }

    /**
     * have at least 3 or more pairs of consecutive ranks
     * */
    private static boolean isConsecutivePairs(List<Card> sorted) {
        int n = sorted.size();
        if (n < THREE_PAIR_RUN_CARDS || n % 2 != 0) return false; // not enough cards OR odd number of cards
        for (int i = 0; i < n; i += 2) {
            if (sorted.get(i).getRank() == Rank.TWO) return false; // cannot be no 2
            if (sorted.get(i).getRank() != sorted.get(i + 1).getRank()) return false; // sorted pairs must be same Rank
            if (i > 0 && !isOneRankAbove(sorted.get(i), sorted.get(i - 2))) return false; // must be consecutive rank of pairs
        }
        return true;
    }

    /** check if b is exactly 1 rank above a, rank of a + 1 == b rank*/
    private static boolean isOneRankAbove(Card higher, Card lower) {
        return higher.getRank().ordinal() == lower.getRank().ordinal() + 1;
    }

    /** check if the new Play candidate beats the Play standing on the table
     * null standing == inital open round
     * */
    public static boolean beats(Play candidate, Play standing) {
        if (standing == null) return true;

        // the normal rule 1 vs 1 situation, same shape, same length, higher top card
        if (candidate.type() == standing.type() && candidate.size() == standing.size()) {
            return candidate.topCard().compareTo(standing.topCard()) > 0;
        }
        // not the same shape, so the only way through is a chop
        return canChop(candidate, standing);
    }


    /** check if a player's Play can chop the standing Play on the table
     *  3 pairs < 4 of a kind < 4 pairs
     *  to flag a chop when it is happening on the table */
    public static boolean canChop(Play candidate, Play standing) {
        if (standing == null) return false;

        // 3 consecutive pairs chop a single 2
        if (isPairRun(candidate, THREE_PAIR_RUN_CARDS)) {
            return isSingle(standing, Rank.TWO);
        }
        // 4 of a kind chops a single 2, or 3 consecutive pairs
        if (candidate.type() == PlayType.FOUR_OF_A_KIND) {
            return isSingle(standing, Rank.TWO)
                    || isPairRun(standing, THREE_PAIR_RUN_CARDS);
        }
        // 4 consecutive pairs chop a single 2, a pair of 2s, a 4 of a kind, or 3 consecutive pairs
        if (isPairRun(candidate, FOUR_PAIR_RUN_CARDS)) {
            return isSingle(standing, Rank.TWO)
                    || isPair(standing, Rank.TWO)
                    || standing.type() == PlayType.FOUR_OF_A_KIND
                    || isPairRun(standing, THREE_PAIR_RUN_CARDS);
        }
        return false;
    }

    private static boolean isPairRun(Play play, int cardCount) {
        return play.type() == PlayType.CONSECUTIVE_PAIRS && play.size() == cardCount;
    }

    private static boolean isSingle(Play play, Rank rank) {
        return play.type() == PlayType.SINGLE && play.topCard().getRank() == rank;
    }


    private static boolean isPair(Play play, Rank rank) {
        return play.type() == PlayType.PAIR && play.topCard().getRank() == rank;
    }

    /** rejection messages for invalid Play*/
    private static String rejectionReason(int cardCount) {
        return switch (cardCount) {
            case 2 -> "pair must be two cards of the same rank (e.g. 10-10)";
            case 3 -> "three cards should form triple (e.g. 3 Queens), or like 3 ranks in a row (e.g. 3-4-5)";
            case 4 -> "four cards should be '4 of a kind' (e.g. 4 Kings) or four ranks in a row (e.g. 4-5-6-7)";
            default -> "cards are not straight? or like cannot contain no 2";
        };
    }

    /** turn CONSECUTIVE_PAIRS into "consecutive pairs" for the error message displaying
     * basically a toString() for PlayType */
    private static String describe(Play play) {
        return play.type().toString().toLowerCase().replace('_', ' ');
    }
}
