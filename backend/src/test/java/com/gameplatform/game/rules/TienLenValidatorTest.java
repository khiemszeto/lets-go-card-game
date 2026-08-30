package com.gameplatform.game.rules;

import static com.gameplatform.game.model.Rank.ACE;
import static com.gameplatform.game.model.Rank.FIVE;
import static com.gameplatform.game.model.Rank.FOUR;
import static com.gameplatform.game.model.Rank.KING;
import static com.gameplatform.game.model.Rank.SIX;
import static com.gameplatform.game.model.Rank.THREE;
import static com.gameplatform.game.model.Rank.TWO;
import static com.gameplatform.game.model.Suit.CLUBS;
import static com.gameplatform.game.model.Suit.DIAMONDS;
import static com.gameplatform.game.model.Suit.HEARTS;
import static com.gameplatform.game.model.Suit.SPADES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.gameplatform.game.model.Card;
import com.gameplatform.game.model.Rank;
import com.gameplatform.game.model.Suit;

public class TienLenValidatorTest {

    private final TienLenValidator validator = new TienLenValidator();

    static Stream<Arguments> validShapes() {
        return Stream.of(
                Arguments.of("single", List.of(c(THREE, SPADES))),
                Arguments.of("pair", List.of(c(THREE, SPADES), c(THREE, CLUBS))),
                Arguments.of("triple", List.of(c(THREE, SPADES), c(THREE, CLUBS), c(THREE, DIAMONDS))),
                Arguments.of("quad", quadOf(THREE)),
                Arguments.of("straight of 3", List.of(c(THREE, SPADES), c(FOUR, SPADES), c(FIVE, SPADES))),
                Arguments.of("3 consecutive pairs", threeSeqPair()),
                Arguments.of("4 consecutive pairs", fourSeqPair()));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validShapes")
    void validShapesAreAccepted(String name, List<Card> cards) {
        assertThat(validator.isValidCombo(cards)).isTrue();
    }

    static Stream<Arguments> invalidShapes() {
        return Stream.of(
                Arguments.of("null", null),
                Arguments.of("empty", List.of()),
                Arguments.of("duplicate cards", List.of(c(THREE, SPADES), c(THREE, SPADES))),
                Arguments.of("straight containing a 2", List.of(c(KING, SPADES), c(ACE, SPADES), c(TWO, SPADES))),
                Arguments.of("non-consecutive ranks", List.of(c(THREE, SPADES), c(FOUR, SPADES), c(SIX, SPADES))),
                Arguments.of("only 2 consecutive pairs",
                        List.of(c(THREE, SPADES), c(THREE, CLUBS), c(FOUR, SPADES), c(FOUR, CLUBS))),
                Arguments.of("pairs with a gap",
                        List.of(c(THREE, SPADES), c(THREE, CLUBS), c(FIVE, SPADES), c(FIVE, CLUBS),
                                c(SIX, SPADES), c(SIX, CLUBS))),
                Arguments.of("two triples, not three pairs",
                        List.of(c(THREE, SPADES), c(THREE, CLUBS), c(THREE, DIAMONDS),
                                c(FOUR, SPADES), c(FOUR, CLUBS), c(FOUR, DIAMONDS))),
                Arguments.of("consecutive pairs containing 2s",
                        List.of(c(KING, SPADES), c(KING, CLUBS), c(ACE, SPADES), c(ACE, CLUBS),
                                c(TWO, SPADES), c(TWO, CLUBS))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidShapes")
    void invalidShapesAreRejected(String name, List<Card> cards) {
        assertThat(validator.isValidCombo(cards)).isFalse();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("validShapes")
    void aComboNeverBeatsItself(String name, List<Card> cards) {
        assertThat(validator.canPlay(cards, cards)).isFalse();
    }

    @Test
    void anyValidComboWinsAFreeLead() {
        List<Card> single = List.of(c(THREE, SPADES));

        assertThat(validator.canPlay(single, null)).isTrue();
        assertThat(validator.canPlay(single, List.of())).isTrue();

        // a free lead still has to be a legal combo
        assertThat(validator.canPlay(List.of(c(THREE, SPADES), c(FOUR, SPADES)), null)).isFalse();
    }

    @Test
    void sameTypeBeatsByRankThenSuit() {
        assertThat(validator.canPlay(List.of(c(FOUR, SPADES)), List.of(c(THREE, SPADES)))).isTrue();
        assertThat(validator.canPlay(List.of(c(THREE, HEARTS)), List.of(c(THREE, SPADES)))).isTrue();
        assertThat(validator.canPlay(List.of(c(THREE, SPADES)), List.of(c(THREE, HEARTS)))).isFalse();

        assertThat(validator.canPlay(
                List.of(c(FOUR, SPADES), c(FOUR, CLUBS)),
                List.of(c(THREE, SPADES), c(THREE, CLUBS)))).isTrue();
    }

    @Test
    void mismatchedTypeOrLengthLoses() {
        // a pair is not a bomb, so it cannot answer a single
        assertThat(validator.canPlay(
                List.of(c(FOUR, SPADES), c(FOUR, CLUBS)),
                List.of(c(THREE, SPADES)))).isFalse();

        // same type, different length
        assertThat(validator.canPlay(
                List.of(c(THREE, SPADES), c(FOUR, SPADES), c(FIVE, SPADES), c(SIX, SPADES)),
                List.of(c(THREE, SPADES), c(FOUR, SPADES), c(FIVE, SPADES)))).isFalse();

        // a quad only bombs 2s, not an ordinary high single
        assertThat(validator.canPlay(quadOf(THREE), List.of(c(ACE, SPADES)))).isFalse();
    }

    @Test
    void bombsBeatTwos() {
        List<Card> singleTwo = List.of(c(TWO, SPADES));
        List<Card> pairOfTwos = List.of(c(TWO, SPADES), c(TWO, CLUBS));

        assertThat(validator.canPlay(threeSeqPair(), singleTwo)).isTrue();
        assertThat(validator.canPlay(quadOf(THREE), singleTwo)).isTrue();
        assertThat(validator.canPlay(quadOf(THREE), pairOfTwos)).isTrue();
        assertThat(validator.canPlay(quadOf(THREE), threeSeqPair())).isTrue();
        assertThat(validator.canPlay(fourSeqPair(), quadOf(KING))).isTrue();

        // 3 consecutive pairs only bomb a single 2
        assertThat(validator.canPlay(threeSeqPair(), pairOfTwos)).isFalse();
        // 4 of a kind bombs single/pair of 2, but Cannot bomb triple 2
        assertThat(validator.canPlay(quadOf(THREE),
                List.of(c(TWO, SPADES), c(TWO, CLUBS), c(TWO, DIAMONDS)))).isFalse();
    }

    @Test
    void fourSeqPairIsTheTopBomb() {
        // 4 consecutive == top of food chain
        List<Card> tripleOfTwos = List.of(c(TWO, SPADES), c(TWO, CLUBS), c(TWO, DIAMONDS));

        assertThat(validator.canPlay(fourSeqPair(), threeSeqPair())).isTrue();
        assertThat(validator.canPlay(fourSeqPair(), tripleOfTwos)).isTrue();
        assertThat(validator.canPlay(fourSeqPair(), quadOf(TWO))).isTrue();

        // still not a wildcard — it cannot answer an ordinary high combo
        assertThat(validator.canPlay(fourSeqPair(), List.of(c(ACE, SPADES)))).isFalse();
    }

    @Test
    void lowestCardBreaksTiesBySuit() {
        // 3 SPADE shoud be the absolute lowest card in the deck since the first player will need to pick this
        assertThat(validator.getLowestCard(List.of(c(FIVE, HEARTS), c(THREE, CLUBS), c(THREE, SPADES))))
                .isEqualTo(c(THREE, SPADES));
    }

    @Test
    void compareCardOrdersByRankThenSuit() {
        assertThat(validator.compareCard(c(FOUR, SPADES), c(THREE, HEARTS))).isPositive(); // rank wins
        assertThat(validator.compareCard(c(THREE, HEARTS), c(THREE, SPADES))).isPositive(); // suit breaks tie
        assertThat(validator.compareCard(c(THREE, SPADES), c(THREE, SPADES))).isZero();
    }

    @Test
    void cardsMayArriveInAnyOrder() {
        // assuming No trust for Front end
        assertThat(validator.isValidCombo(List.of(c(FIVE, SPADES), c(THREE, SPADES), c(FOUR, SPADES)))).isTrue();
        assertThat(validator.isValidCombo(List.of(
                c(FIVE, CLUBS), c(THREE, SPADES), c(FOUR, CLUBS),
                c(FOUR, SPADES), c(FIVE, SPADES), c(THREE, CLUBS)))).isTrue(); // shuffled 3 seq pairs
    }

    // static factory to construct card
    private static Card c(Rank rank, Suit suit) {
        return new Card(suit, rank);
    }

    private static List<Card> quadOf(Rank rank) {
        return List.of(c(rank, SPADES), c(rank, CLUBS), c(rank, DIAMONDS), c(rank, HEARTS));
    }

    private static List<Card> threeSeqPair() {
        return List.of(c(THREE, SPADES), c(THREE, CLUBS),
                c(FOUR, SPADES), c(FOUR, CLUBS),
                c(FIVE, SPADES), c(FIVE, CLUBS));
    }

    private static List<Card> fourSeqPair() {
        return List.of(c(THREE, SPADES), c(THREE, CLUBS),
                c(FOUR, SPADES), c(FOUR, CLUBS),
                c(FIVE, SPADES), c(FIVE, CLUBS),
                c(SIX, SPADES), c(SIX, CLUBS));
    }
}
