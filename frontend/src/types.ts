export interface PlayerSession {
  id: number;
  username: string;
  local?: boolean;
}

export interface RoomPlayer {
  playerId: number;
  username: string;
  seat: number;
  ready: boolean;
  numberOfCards?: number;
}

export interface RoomSummary {
  roomId: string;
  hostUsername: string;
  playerCount: number;
  maxPlayers: number;
  readyPlayers: number;
  status: "WAITING" | "COUNTING_DOWN" | "STARTED";
  joinable: boolean;
}

export interface RoomStateMessage {
  type: "ROOM_STATE";
  roomId: string;
  players: RoomPlayer[];
  readyPlayers: number;
}

export interface CountdownMessage {
  type: "COUNTDOWN";
  roomId: string;
  seconds: number;
}

export interface LeaveRoomMessage {
  type: "LEAVE_ROOM";
  roomId: string;
}

export interface Card {
  suit: "SPADE" | "CLUBS" | "DIAMONDS" | "HEARTS";
  rank:
    | "THREE"
    | "FOUR"
    | "FIVE"
    | "SIX"
    | "SEVEN"
    | "EIGHT"
    | "NINE"
    | "TEN"
    | "JACK"
    | "QUEEN"
    | "KING"
    | "ACE"
    | "TWO";
}

export interface HandMessage {
  type: "HAND";
  roomId: string;
  cards: Card[];
}

export interface GameStartMessage {
  type: "GAME_START";
  roomId: string;
  players: RoomPlayer[];
  currentPlayerId: number;
}

export interface PlayResultMessage {
  type: "PLAY_RESULT";
  roomId: string;
  playerId: number;
  currentPlayerId: number;
  players: RoomPlayer[];
  trickReset: boolean;
  cardsPlayed: Card[];
}

export interface PassResultMessage {
  type: "PASS_RESULT";
  roomId: string;
  playerId: number;
  currentPlayerId: number;
  trickReset: boolean;
}

export interface GameOverMessage {
  type: "GAME_OVER";
  roomId: string;
  winnerId: number;
}

export interface LeftDuringGameMessage {
  type: "LEFT_DURING_GAME";
  roomId: string;
  leftPlayerId: number;
  leftPlayerUsername: string;
  currentPlayerId: number;
  players: RoomPlayer[];
  trickReset: boolean;
  autoPassed: boolean;
}

export interface ErrorMessage {
  type?: string;
  message: string;
}

export interface LastPlay {
  playerId: number;
  username: string;
  cards: Card[];
}

export interface GameOverState {
  winnerId: number;
  winnerUsername: string;
}

export interface GameState {
  roomId: string;
  players: RoomPlayer[];
  currentPlayerId: number;
  myHand: Card[];
  selectedCardKeys: string[];
  lastPlay: LastPlay | null;
  gameOver: GameOverState | null;
}

/** Page 1: login · Page 2: lobby (tables or waiting) · Page 3: game */
export type Screen = "login" | "lobby" | "game";

export type LobbyMode = "tables" | "waiting";

export type InboundMessage =
  | RoomStateMessage
  | CountdownMessage
  | LeaveRoomMessage
  | LeftDuringGameMessage
  | HandMessage
  | ErrorMessage
  | GameStartMessage
  | PlayResultMessage
  | PassResultMessage
  | GameOverMessage
  | { type: string; [key: string]: unknown };
