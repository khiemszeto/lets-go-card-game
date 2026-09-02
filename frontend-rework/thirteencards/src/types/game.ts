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