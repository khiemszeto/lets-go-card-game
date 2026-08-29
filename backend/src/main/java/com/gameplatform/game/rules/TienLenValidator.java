package com.gameplatform.game.rules;

import com.gameplatform.game.model.Card;
import com.gameplatform.game.model.Combo;
import com.gameplatform.game.model.ComboType;
import com.gameplatform.game.model.Rank;
import com.gameplatform.game.model.Suit;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pure Tiến Lên (Southern-style) rules engine.
 * <p>
 * Answers two questions only:
 * <ol>
 *   <li>Is this list of cards a legal combo?</li>
 *   <li>Does {@code played} beat {@code lastPlay} on the table?</li>
 * </ol>
 * <p>
 * <b>Supported combos:</b> SINGLE, PAIR, TRIPLE, STRAIGHT (≥3, no 2s),
 * SEQ_PAIR (exactly 3 or 4 consecutive pairs, no 2s), QUAD.
 * <p>
 * <b>Strength:</b> rank via {@link Rank} ordinal (3 low → 2 high),
 * suit via {@link Suit} ordinal (SPADE → HEARTS).
 * Same-type beats require same type + same card count; winner is higher high card
 * (rank first, then suit).
 * <p>
 * <b>Bombs (cross-type):</b>
 * <ul>
 *   <li>3 SEQ_PAIR → beats single 2 only</li>
 *   <li>QUAD → beats 3 SEQ_PAIR, single 2, pair of 2s</li>
 *   <li>4 SEQ_PAIR → beats 3 SEQ_PAIR, any QUAD, any 2s combo</li>
 * </ul>
 * <p>
 * <b>Not handled here</b> (belongs in {@code GameService}):
 * turn order, cards-in-hand ownership, opening 3♠ rule, holding quad of 2s auto-win.
 * <p>
 * Stateless — no Spring, no {@code Room}, no WebSocket.
 */

@Component
public class TienLenValidator {
    /**
     * Returns whether {@code cards} form a legal Tiến Lên combo.
     *
     * @param cards cards proposed for play; may be null
     * @return {@code true} if {@link #classify(List)} is not {@link ComboType#INVALID}
     */
    public boolean isValidCombo(List<Card> cards) {
        return classify(cards).getType() != ComboType.INVALID;
    }

    /**
     * Returns whether {@code played} may be placed on the table given {@code lastPlay}.
     * <p>
     * Free lead ({@code lastPlay} null or empty): any valid combo is allowed.
     * Otherwise {@code played} must beat {@code lastPlay} via same-type comparison
     * or bomb/chop rules ({@link #canBombBeat(Combo, Combo)}).
     *
     * @param played   cards the player wants to play
     * @param lastPlay cards currently on the table for this trick, or null/empty for free lead
     * @return {@code true} if the play is legal relative to the table
     */
    public boolean canPlay(List<Card> played, List<Card> lastPlay) {
        Combo p = classify(played);
        if (p.getType() == ComboType.INVALID) return false;

        if (lastPlay == null || lastPlay.isEmpty()) return true;

        Combo last = classify(lastPlay);

        if (p.getType() == last.getType()
                && p.getCards().size() == last.getCards().size()) {
            return beatSameType(p, last);
        }

        return canBombBeat(p, last);
    }

    /**
     * Cross-type bomb/chop beats when same-type comparison does not apply.
     * <p>
     * Only bombs beat across types; normal singles/pairs/straights cannot.
     *
     * @param p    classified combo being played
     * @param last classified combo on the table
     * @return {@code true} if {@code p} is a bomb that beats {@code last}
     */
    private boolean canBombBeat(Combo p, Combo last) {
        if (isThreeSeqPair(p) && isSingleTwo(last)) {
            return true;
        }
        if (p.getType() == ComboType.QUAD) {
            if (isThreeSeqPair(last)) return true;
            if (isSingleTwo(last)) return true;
            if (isPairOfTwos(last)) return true;
            return false;
        }
        if (isFourSeqPair(p)) {
            if (isThreeSeqPair(last)) return true;
            if (last.getType() == ComboType.QUAD) return true;
            if (isAnyTwosCombo(last)) return true;
            return false;
        }
        return false;
    }

    /**
     * Same {@link ComboType} and same card count: {@code p} wins if its high card
     * is strictly stronger (higher rank, or same rank with higher suit).
     *
     * @param p    played combo
     * @param last combo on the table
     * @return {@code true} if {@code p} beats {@code last}
     */
    private boolean beatSameType(Combo p, Combo last) {
        if (p.getHighRank().ordinal() > last.getHighRank().ordinal()) return true;
        return p.getHighRank().ordinal() == last.getHighRank().ordinal()
                && p.getHighSuit().ordinal() > last.getHighSuit().ordinal();
    }

    /**
     * Compares two cards: rank first ({@code THREE} … {@code TWO}), then suit
     * ({@code SPADE} … {@code HEARTS}).
     *
     * @return negative if {@code a} is weaker, zero if equal, positive if {@code a} is stronger
     */
    public int compareCard(Card a, Card b) {
        int byRank = Integer.compare(a.getRank().ordinal(), b.getRank().ordinal());
        if (byRank != 0) return byRank;
        return Integer.compare(a.getSuit().ordinal(), b.getSuit().ordinal());
    }

    /**
     * Returns a new list sorted weakest → strongest by {@link #compareCard(Card, Card)}.
     */
    private List<Card> sortedCopy(List<Card> cards) {
        List<Card> copy = new ArrayList<>(cards);
        copy.sort(this::compareCard);
        return copy;
    }

    public Card getLowestCard(List<Card> cards) {
        return sortedCopy(cards).get(0);
    }

    /**
     * Returns {@code true} if any card in the list has rank {@link Rank#TWO}.
     * Straights and seq-pairs must not contain 2s.
     */
    private boolean containsTwo(List<Card> cards) {
        return cards.stream().anyMatch(c -> c.getRank() == Rank.TWO);
    }

    /** Sentinel {@link Combo} for illegal or unclassifiable input. */
    private Combo invalid() {
        return new Combo(ComboType.INVALID, List.of(), null, null);
    }

    /**
     * Classifies {@code cards} into a {@link Combo} with type, sorted cards,
     * and high rank/suit for comparison.
     * <p>
     * Rejects null, empty, or duplicate cards. Detection order:
     * SINGLE → same-rank (PAIR/TRIPLE/QUAD) → SEQ_PAIR → STRAIGHT → INVALID.
     *
     * @param cards raw cards (not necessarily sorted)
     * @return classified combo, or {@link ComboType#INVALID}
     */
    private Combo classify(List<Card> cards) {
        if (cards == null || cards.isEmpty()) return invalid();

        Set<Card> unique = new HashSet<>(cards);
        if (unique.size() != cards.size()) return invalid();

        List<Card> sorted = sortedCopy(cards);
        Card highCard = sorted.get(sorted.size() - 1);
        int n = sorted.size();

        if (n == 1) {
            return new Combo(
                    ComboType.SINGLE, sorted, highCard.getRank(), highCard.getSuit());
        }

        boolean allSameRank
                = sorted.stream().allMatch(c -> c.getRank() == sorted.get(0).getRank());
        if (allSameRank) {
            ComboType type = switch (n) {
                case 2 -> ComboType.PAIR;
                case 3 -> ComboType.TRIPLE;
                case 4 -> ComboType.QUAD;
                default -> ComboType.INVALID;
            };
            if (type == ComboType.INVALID) return invalid();

            return new Combo(type, sorted, highCard.getRank(), highCard.getSuit());
        }

        Combo seq = trySeqPairs(sorted);
        if (seq != null) return seq;

        Combo straight = tryStraight(sorted);
        if (straight != null) return straight;

        return invalid();
    }

    /**
     * Attempts to classify sorted cards as a STRAIGHT (≥3 consecutive ranks, all distinct, no 2s).
     *
     * @param sorted cards sorted by {@link #compareCard(Card, Card)}
     * @return STRAIGHT combo, or {@code null} if not a straight
     */
    private Combo tryStraight(List<Card> sorted) {
        int n = sorted.size();
        if (n < 3) return null;
        if (containsTwo(sorted)) return null;

        for (int i = 0; i + 1 < n; i++) {
            int r0 = sorted.get(i).getRank().ordinal();
            int r1 = sorted.get(i + 1).getRank().ordinal();
            if (r0 + 1 != r1) return null;
        }

        Card highCard = sorted.get(n - 1);
        return new Combo(
                ComboType.STRAIGHT, sorted, highCard.getRank(), highCard.getSuit());
    }

    /**
     * Attempts to classify sorted cards as SEQ_PAIR (3 or 4 consecutive pairs, no 2s).
     * Only 6 cards (3 pairs) or 8 cards (4 pairs) are accepted.
     *
     * @param sorted cards sorted by {@link #compareCard(Card, Card)}
     * @return SEQ_PAIR combo, or {@code null} if not valid seq-pairs
     */
    private Combo trySeqPairs(List<Card> sorted) {
        int n = sorted.size();

        if (n != 6 && n != 8) return null;
        if (containsTwo(sorted)) return null;

        for (int i = 0; i < n; i += 2) {
            if (sorted.get(i).getRank() != sorted.get(i + 1).getRank()) return null;
        }
        for (int i = 0; i + 2 < n; i += 2) {
            if (sorted.get(i).getRank().ordinal() + 1 != sorted.get(i + 2).getRank().ordinal()) {
                return null;
            }
        }
        Card high = sorted.get(n - 1);
        return new Combo(ComboType.SEQ_PAIR, sorted, high.getRank(), high.getSuit());
    }

    /** {@code true} if combo is a single card with rank {@link Rank#TWO}. */
    private boolean isSingleTwo(Combo c) {
        return c.getType() == ComboType.SINGLE && c.getHighRank() == Rank.TWO;
    }

    /** {@code true} if combo is a pair of 2s. */
    private boolean isPairOfTwos(Combo c) {
        return c.getType() == ComboType.PAIR && c.getHighRank() == Rank.TWO;
    }

    /** {@code true} if combo is a triple of 2s. */
    private boolean isTripleOfTwos(Combo c) {
        return c.getType() == ComboType.TRIPLE && c.getHighRank() == Rank.TWO;
    }

    /** {@code true} if combo is a quad of 2s. */
    private boolean isQuadOfTwos(Combo c) {
        return c.getType() == ComboType.QUAD && c.getHighRank() == Rank.TWO;
    }

    /**
     * {@code true} if every card in the combo is rank {@link Rank#TWO}
     * (single, pair, triple, or quad of 2s).
     */
    private boolean isAnyTwosCombo(Combo c) {
        return c.getHighRank() == Rank.TWO;
    }

    /** {@code true} if combo is exactly 3 consecutive pairs (6 cards). */
    private boolean isThreeSeqPair(Combo c) {
        return c.getType() == ComboType.SEQ_PAIR && c.getCards().size() == 6;
    }

    /** {@code true} if combo is exactly 4 consecutive pairs (8 cards). */
    private boolean isFourSeqPair(Combo c) {
        return c.getType() == ComboType.SEQ_PAIR && c.getCards().size() == 8;
    }
}