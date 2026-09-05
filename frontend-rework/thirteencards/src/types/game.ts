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
    type: 'ROOM_STATE';
    roomId: string;
    players: RoomPlayer[];
    readyPlayers: number;
}

export interface RoomPlayer {
    playerId: number;
    username: string;
    numberOfCards: number;
    ready: boolean;
    seat: number;
}

export interface ErrorMessage {
    type: 'ERROR';
    message: string;
}

export interface LeaveRoomMessage {
    type: 'LEAVE_ROOM'
    roomId: string
}

export interface CountDownMessage {
    type: 'COUNTDOWN';
    roomId: string;
    seconds: number;
}

export interface Card {
    suit: 'SPADES' | 'HEARTS' | 'DIAMONDS' | 'CLUBS';
    rank: 'THREE' | 'FOUR' | 'FIVE' | 'SIX' | 'SEVEN' | 'EIGHT' | 'NINE' | 'TEN' | 'JACK' | 'QUEEN' | 'KING' | 'ACE' | 'TWO';
}

export interface GameStartMessage {
    type: 'GAME_START';
    roomId: string;
    players: RoomPlayer[];
    currentPlayerId: number;
}

export interface HandMessage {
    type: 'HAND'
    roomId: string
    cards: Card[]
}
export interface PlayResultMessage {
    type: 'PLAY_RESULT'
    roomId: string
    playerId: number
    currentPlayerId: number
    players: RoomPlayer[]
    trickReset: boolean
    cardsPlayed: Card[]
}
export interface PassResultMessage {
    type: 'PASS_RESULT'
    roomId: string
    playerId: number
    currentPlayerId: number
    trickReset: boolean
}
export interface BalanceChange {
    playerName: string
    delta: number
    newBalance: number
}

export interface GameOverMessage {
    type: 'GAME_OVER'
    roomId: string
    winnerId: number
    balances: BalanceChange[]
}
export interface LeftDuringGameMessage {
    type: 'LEFT_DURING_GAME'
    roomId: string
    leftPlayerId: number
    leftPlayerUsername: string
    currentPlayerId: number
    players: RoomPlayer[]
    trickReset: boolean
    autoPassed: boolean
}