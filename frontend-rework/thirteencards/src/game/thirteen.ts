import { Card } from '@magmacrunch/adenosine-cards'
import type { Rank, Suit } from '@magmacrunch/adenosine-cards'

/**
 * rank order, lowest to highest.
 *
 * Not the standard ace-high order the library ships: a 3 is the weakest card
 * in Thirteen and a 2 the strongest, so the ladder wraps around past the ace.
 */
export const THIRTEEN_RANKS: readonly Rank[] = [
    '3', '4', '5', '6', '7', '8', '9', '10', 'J', 'Q', 'K', 'A', '2',
]

/** Tiến lên suit order, lowest to highest. Spades are weakest, hearts strongest. */
export const THIRTEEN_SUITS: readonly Suit[] = [
    'spades', 'clubs', 'diamonds', 'hearts',
]

/**
 * All 52 cards in ascending Thirteen order — 3♠ first, 2♥ last.
 *
 * Rank leads and suit breaks the tie, which is how the game compares two
 * single cards. Every card is flipped face up; the library builds them
 * face down.
 */
export function orderedDeck(): Card[] {
    return THIRTEEN_RANKS.flatMap((rank) =>
        THIRTEEN_SUITS.map((suit) => {
            const card = new Card(suit, rank)
            card.flip()
            return card
        }),
    )
}
