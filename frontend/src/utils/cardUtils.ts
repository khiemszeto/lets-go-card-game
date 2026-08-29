import type { Card } from "../types";

const RANK_ORDER: Record<Card["rank"], number> = {
  THREE: 0,
  FOUR: 1,
  FIVE: 2,
  SIX: 3,
  SEVEN: 4,
  EIGHT: 5,
  NINE: 6,
  TEN: 7,
  JACK: 8,
  QUEEN: 9,
  KING: 10,
  ACE: 11,
  TWO: 12,
};

const SUIT_ORDER: Record<Card["suit"], number> = {
  SPADE: 0,
  CLUBS: 1,
  DIAMONDS: 2,
  HEARTS: 3,
};

const RANK_LABEL: Record<Card["rank"], string> = {
  THREE: "3",
  FOUR: "4",
  FIVE: "5",
  SIX: "6",
  SEVEN: "7",
  EIGHT: "8",
  NINE: "9",
  TEN: "10",
  JACK: "J",
  QUEEN: "Q",
  KING: "K",
  ACE: "A",
  TWO: "2",
};

const SUIT_SYMBOL: Record<Card["suit"], string> = {
  SPADE: "♠",
  CLUBS: "♣",
  DIAMONDS: "♦",
  HEARTS: "♥",
};

export function cardKey(card: Card): string {
  return `${card.suit}:${card.rank}`;
}

export function sortHand(cards: Card[]): Card[] {
  return [...cards].sort((a, b) => {
    const rankDiff = RANK_ORDER[a.rank] - RANK_ORDER[b.rank];
    if (rankDiff !== 0) return rankDiff;
    return SUIT_ORDER[a.suit] - SUIT_ORDER[b.suit];
  });
}

export function rankLabel(rank: Card["rank"]): string {
  return RANK_LABEL[rank];
}

export function suitSymbol(suit: Card["suit"]): string {
  return SUIT_SYMBOL[suit];
}

export function isRedSuit(suit: Card["suit"]): boolean {
  return suit === "DIAMONDS" || suit === "HEARTS";
}

export function cardsFromKeys(hand: Card[], keys: string[]): Card[] {
  const selected = new Set(keys);
  return hand.filter((card) => selected.has(cardKey(card)));
}
