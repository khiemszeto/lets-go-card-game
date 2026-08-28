import type { Card, GameState, RoomPlayer } from "../types";
import {
  cardKey,
  isRedSuit,
  rankLabel,
  sortHand,
  suitSymbol,
} from "../utils/cardUtils";
import { bindLeaveButton, renderPageShell } from "./pageShell";

const SEAT_POSITIONS = ["seat-bottom", "seat-left", "seat-top", "seat-right"];

function escapeHtml(text: string): string {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}

function orderedSeats(players: RoomPlayer[], myId: number): { player: RoomPlayer; position: string }[] {
  const sorted = [...players].sort((a, b) => a.seat - b.seat);
  const myIdx = sorted.findIndex((p) => p.playerId === myId);
  const rotated = myIdx === -1 ? sorted : [...sorted.slice(myIdx), ...sorted.slice(0, myIdx)];

  return rotated.map((player, index) => ({
    player,
    position: SEAT_POSITIONS[index] ?? "seat-top",
  }));
}

function renderMiniCard(card: Card): string {
  const red = isRedSuit(card.suit);
  return `
    <span class="mini-card ${red ? "mini-card--red" : "mini-card--black"}">
      <span class="mini-card__rank">${rankLabel(card.rank)}</span>
      <span class="mini-card__suit">${suitSymbol(card.suit)}</span>
    </span>
  `;
}

function renderPlayingCard(card: Card, selected: boolean): string {
  const red = isRedSuit(card.suit);
  const key = cardKey(card);
  return `
    <button
      class="playing-card ${red ? "playing-card--red" : "playing-card--black"} ${selected ? "playing-card--selected" : ""}"
      type="button"
      data-card-key="${key}"
      data-suit="${card.suit}"
      data-rank="${card.rank}"
      aria-pressed="${selected}"
    >
      <span class="playing-card__corner playing-card__corner--tl">
        <span class="playing-card__rank">${rankLabel(card.rank)}</span>
        <span class="playing-card__suit">${suitSymbol(card.suit)}</span>
      </span>
      <span class="playing-card__center">${suitSymbol(card.suit)}</span>
      <span class="playing-card__corner playing-card__corner--br">
        <span class="playing-card__rank">${rankLabel(card.rank)}</span>
        <span class="playing-card__suit">${suitSymbol(card.suit)}</span>
      </span>
    </button>
  `;
}

function turnLabel(game: GameState, myId: number): string {
  if (game.gameOver) return `${game.gameOver.winnerUsername} wins the round!`;
  if (game.currentPlayerId === myId) return "Your turn — select cards and play, or pass";
  const current = game.players.find((p) => p.playerId === game.currentPlayerId);
  return current ? `${current.username}'s turn` : "Waiting for next player…";
}

function canPass(game: GameState, myId: number): boolean {
  return (
    !game.gameOver &&
    game.currentPlayerId === myId &&
    game.lastPlay !== null &&
    game.lastPlay.cards.length > 0
  );
}

function canPlay(game: GameState, myId: number): boolean {
  return !game.gameOver && game.currentPlayerId === myId;
}

export function renderGame(
  root: HTMLElement,
  username: string,
  playerId: number,
  game: GameState,
  statusText: string,
  error: string | null,
  wsConnected: boolean,
  onToggleCard: (suit: string, rank: string) => void,
  onPlay: () => void,
  onPass: () => void,
  onClear: () => void,
  onLeave: () => void,
): void {
  const selected = new Set(game.selectedCardKeys);
  const myTurn = canPlay(game, playerId);
  const passEnabled = canPass(game, playerId);
  const hand = sortHand(game.myHand);

  const opponents = orderedSeats(game.players, playerId).filter(({ player }) => player.playerId !== playerId);

  const seatHtml = opponents
    .map(({ player, position }) => {
      const isTurn = game.currentPlayerId === player.playerId;
      return `
        <div class="game-seat ${position} ${isTurn ? "game-seat--turn" : ""}">
          <div class="game-seat__avatar">${escapeHtml(player.username.slice(0, 1).toUpperCase())}</div>
          <div class="game-seat__info">
            <strong>${escapeHtml(player.username)}</strong>
            <span>${player.numberOfCards ?? 0} cards</span>
            ${isTurn ? `<span class="turn-pill">Turn</span>` : ""}
          </div>
        </div>
      `;
    })
    .join("");

  const pileHtml =
    game.lastPlay && game.lastPlay.cards.length > 0
      ? `
        <div class="trick-pile">
          <p class="trick-pile__label">${escapeHtml(game.lastPlay.username)} played</p>
          <div class="trick-pile__cards">
            ${game.lastPlay.cards.map((card) => renderMiniCard(card)).join("")}
          </div>
        </div>
      `
      : `<div class="trick-pile trick-pile--empty"><p class="trick-pile__label">Free lead</p></div>`;

  const handHtml =
    hand.length > 0
      ? hand
          .map((card) => renderPlayingCard(card, selected.has(cardKey(card))))
          .join("")
      : `<p class="hand-empty">Waiting for your hand…</p>`;

  const banners: string[] = [];
  if (!wsConnected) banners.push(`<p class="status-banner">Reconnecting to server…</p>`);
  if (statusText) banners.push(`<p class="status-banner">${escapeHtml(statusText)}</p>`);
  if (error) banners.push(`<p class="error-banner">${escapeHtml(error)}</p>`);

  const gameOverOverlay = game.gameOver
    ? `
      <div class="game-over-overlay">
        <div class="game-over-card">
          <p class="eyebrow">Round over</p>
          <h2>${escapeHtml(game.gameOver.winnerUsername)} wins!</h2>
          <p class="subtitle">Returning to the table shortly…</p>
        </div>
      </div>
    `
    : "";

  const body = `
    <p class="turn-banner ${myTurn && !game.gameOver ? "turn-banner--mine" : ""}">
      ${escapeHtml(turnLabel(game, playerId))}
    </p>
    <div class="game-table-wrap">
      <div class="game-table">
        ${gameOverOverlay}
        ${seatHtml}
        <div class="game-center">${pileHtml}</div>
      </div>
    </div>
    <div class="hand-panel">
      <div class="hand-panel__actions">
        <button id="play-btn" class="btn btn-primary" type="button" ${!myTurn || selected.size === 0 ? "disabled" : ""}>Play</button>
        <button id="pass-btn" class="btn btn-secondary" type="button" ${!passEnabled ? "disabled" : ""}>Pass</button>
        <button id="clear-btn" class="btn btn-ghost" type="button" ${selected.size === 0 ? "disabled" : ""}>Clear</button>
        <span class="hand-panel__hint">${selected.size} selected</span>
      </div>
      <div id="hand-cards" class="hand-cards">${handHtml}</div>
    </div>
  `;

  root.innerHTML = renderPageShell(
    "Game Table",
    "Tiến Lên",
    username,
    "Leave",
    body,
    banners.join(""),
  );

  bindLeaveButton(root, onLeave);

  root.querySelector("#play-btn")?.addEventListener("click", onPlay);
  root.querySelector("#pass-btn")?.addEventListener("click", onPass);
  root.querySelector("#clear-btn")?.addEventListener("click", onClear);

  root.querySelector("#hand-cards")?.addEventListener("click", (event) => {
    const target = (event.target as HTMLElement).closest<HTMLButtonElement>(".playing-card");
    if (!target || !myTurn || game.gameOver) return;
    onToggleCard(target.dataset.suit!, target.dataset.rank!);
  });
}
