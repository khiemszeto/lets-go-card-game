import type { Card } from '../types/game'

const RANK_ORDER: Record<Card['rank'], number> = {
    THREE: 0, FOUR: 1, FIVE: 2, SIX: 3, SEVEN: 4, EIGHT: 5,
    NINE: 6, TEN: 7, JACK: 8, QUEEN: 9, KING: 10, ACE: 11, TWO: 12,
}

const SUIT_ORDER: Record<Card['suit'], number> = {
    SPADES: 0, CLUBS: 1, DIAMONDS: 2, HEARTS: 3,
}

const RANK_LABEL: Record<Card['rank'], string> = {
    THREE: '3', FOUR: '4', FIVE: '5', SIX: '6', SEVEN: '7', EIGHT: '8',
    NINE: '9', TEN: '10', JACK: 'J', QUEEN: 'Q', KING: 'K', ACE: 'A', TWO: '2',
}

const SUIT_SYMBOL: Record<Card['suit'], string> = {
    SPADES: '♠', CLUBS: '♣', DIAMONDS: '♦', HEARTS: '♥',
}

export function cardKey(card: Card) {
    return `${card.suit}:${card.rank}`
}

export function sortHand(cards: Card[]) {
    return [...cards].sort(
        (a, b) => {
            const d = RANK_ORDER[a.rank] - RANK_ORDER[b.rank]
            return d !== 0 ? d : (SUIT_ORDER[a.suit] - SUIT_ORDER[b.suit])}
    )
}

export function rankLabel(rank: Card['rank']) {
    return RANK_LABEL[rank]
}

export function suitSymbol(suit: Card['suit']) {
    return SUIT_SYMBOL[suit]
}

export function isRedSuit(suit: Card['suit']) {
    return suit === 'DIAMONDS' || suit === 'HEARTS'
}

export function sameCard(a: Card, b: Card) {
    return a.suit === b.suit && a.rank === b.rank
}