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
import static com.gameplatform.game.model.Suit.SPADE;
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
                Arguments.of("single", List.of(c(THREE, SPADE))),
                Arguments.of("pair", List.of(c(THREE, SPADE), c(THREE, CLUBS))),
                Arguments.of("triple", List.of(c(THREE, SPADE), c(THREE, CLUBS), c(THREE, DIAMONDS))),
                Arguments.of("quad", quadOf(THREE)),
                Arguments.of("straight of 3", List.of(c(THREE, SPADE), c(FOUR, SPADE), c(FIVE, SPADE))),
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
                Arguments.of("duplicate cards", List.of(c(THREE, SPADE), c(THREE, SPADE))),
                Arguments.of("straight containing a 2", List.of(c(KING, SPADE), c(ACE, SPADE), c(TWO, SPADE))),
                Arguments.of("non-consecutive ranks", List.of(c(THREE, SPADE), c(FOUR, SPADE), c(SIX, SPADE))),
                Arguments.of("only 2 consecutive pairs",
                        List.of(c(THREE, SPADE), c(THREE, CLUBS), c(FOUR, SPADE), c(FOUR, CLUBS))),
                Arguments.of("consecutive pairs containing 2s",
                        List.of(c(KING, SPADE), c(KING, CLUBS), c(ACE, SPADE), c(ACE, CLUBS),
                                c(TWO, SPADE), c(TWO, CLUBS))));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidShapes")
    void invalidShapesAreRejected(String name, List<Card> cards) {
        assertThat(validator.isValidCombo(cards)).isFalse();
    }

    @Test
    void anyValidComboWinsAFreeLead() {
        List<Card> single = List.of(c(THREE, SPADE));

        assertThat(validator.canPlay(single, null)).isTrue();
        assertThat(validator.canPlay(single, List.of())).isTrue();

        // a free lead still has to be a legal combo
        assertThat(validator.canPlay(List.of(c(THREE, SPADE), c(FOUR, SPADE)), null)).isFalse();
    }

    @Test
    void sameTypeBeatsByRankThenSuit() {
        assertThat(validator.canPlay(List.of(c(FOUR, SPADE)), List.of(c(THREE, SPADE)))).isTrue();
        assertThat(validator.canPlay(List.of(c(THREE, HEARTS)), List.of(c(THREE, SPADE)))).isTrue();
        assertThat(validator.canPlay(List.of(c(THREE, SPADE)), List.of(c(THREE, HEARTS)))).isFalse();

        assertThat(validator.canPlay(
                List.of(c(FOUR, SPADE), c(FOUR, CLUBS)),
                List.of(c(THREE, SPADE), c(THREE, CLUBS)))).isTrue();
    }

    @Test
    void mismatchedTypeOrLengthLoses() {
        // a pair is not a bomb, so it cannot answer a single
        assertThat(validator.canPlay(
                List.of(c(FOUR, SPADE), c(FOUR, CLUBS)),
                List.of(c(THREE, SPADE)))).isFalse();

        // same type, different length
        assertThat(validator.canPlay(
                List.of(c(THREE, SPADE), c(FOUR, SPADE), c(FIVE, SPADE), c(SIX, SPADE)),
                List.of(c(THREE, SPADE), c(FOUR, SPADE), c(FIVE, SPADE)))).isFalse();

        // a quad only bombs 2s, not an ordinary high single
        assertThat(validator.canPlay(quadOf(THREE), List.of(c(ACE, SPADE)))).isFalse();
    }

    @Test
    void bombsBeatTwos() {
        List<Card> singleTwo = List.of(c(TWO, SPADE));
        List<Card> pairOfTwos = List.of(c(TWO, SPADE), c(TWO, CLUBS));

        assertThat(validator.canPlay(threeSeqPair(), singleTwo)).isTrue();
        assertThat(validator.canPlay(quadOf(THREE), singleTwo)).isTrue();
        assertThat(validator.canPlay(quadOf(THREE), pairOfTwos)).isTrue();
        assertThat(validator.canPlay(quadOf(THREE), threeSeqPair())).isTrue();
        assertThat(validator.canPlay(fourSeqPair(), quadOf(KING))).isTrue();

        // 3 consecutive pairs only bomb a single 2
        assertThat(validator.canPlay(threeSeqPair(), pairOfTwos)).isFalse();
    }

    // static factory to construct card
    private static Card c(Rank rank, Suit suit) {
        return new Card(suit, rank);
    }

    private static List<Card> quadOf(Rank rank) {
        return List.of(c(rank, SPADE), c(rank, CLUBS), c(rank, DIAMONDS), c(rank, HEARTS));
    }

    private static List<Card> threeSeqPair() {
        return List.of(c(THREE, SPADE), c(THREE, CLUBS),
                c(FOUR, SPADE), c(FOUR, CLUBS),
                c(FIVE, SPADE), c(FIVE, CLUBS));
    }

    private static List<Card> fourSeqPair() {
        return List.of(c(THREE, SPADE), c(THREE, CLUBS),
                c(FOUR, SPADE), c(FOUR, CLUBS),
                c(FIVE, SPADE), c(FIVE, CLUBS),
                c(SIX, SPADE), c(SIX, CLUBS));
    }
}
