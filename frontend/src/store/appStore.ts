import type {
  CountdownMessage,
  GameOverMessage,
  GameStartMessage,
  GameState,
  HandMessage,
  LeftDuringGameMessage,
  LobbyMode,
  PassResultMessage,
  PlayResultMessage,
  RoomStateMessage,
  RoomSummary,
  Screen,
  PlayerSession,
} from "../types";
import { cardKey } from "../utils/cardUtils";

export interface AppState {
  screen: Screen;
  lobbyMode: LobbyMode;
  player: PlayerSession | null;
  rooms: RoomSummary[];
  roomState: RoomStateMessage | null;
  countdown: number | null;
  statusText: string;
  error: string | null;
  wsConnected: boolean;
  game: GameState | null;
}

type Listener = () => void;

const initialState: AppState = {
  screen: "login",
  lobbyMode: "tables",
  player: null,
  rooms: [],
  roomState: null,
  countdown: null,
  statusText: "",
  error: null,
  wsConnected: false,
  game: null,
};

class AppStore {
  private state: AppState = { ...initialState };
  private listeners = new Set<Listener>();

  subscribe(listener: Listener): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  getState(): AppState {
    return this.state;
  }

  private emit(): void {
    for (const listener of this.listeners) listener();
  }

  patch(partial: Partial<AppState>): void {
    this.state = { ...this.state, ...partial };
    this.emit();
  }

  setPlayer(player: PlayerSession): void {
    this.patch({
      player,
      screen: "lobby",
      lobbyMode: "tables",
      error: null,
    });
  }

  setWsConnected(connected: boolean): void {
    this.patch({ wsConnected: connected });
  }

  setRooms(rooms: RoomSummary[]): void {
    this.patch({ rooms });
  }

  upsertRoom(summary: RoomSummary): void {
    const idx = this.state.rooms.findIndex((r) => r.roomId === summary.roomId);
    const rooms =
      idx === -1
        ? [...this.state.rooms, summary]
        : this.state.rooms.map((r, i) => (i === idx ? { ...r, ...summary } : r));
    this.patch({ rooms });
  }

  enterRoom(roomState: RoomStateMessage): void {
    this.patch({
      screen: "lobby",
      lobbyMode: "waiting",
      roomState,
      countdown: null,
      error: null,
      statusText: "",
      game: null,
    });
  }

  updateRoomState(roomState: RoomStateMessage): void {
    this.patch({ roomState, error: null, game: null });
  }

  setCountdown(countdown: CountdownMessage): void {
    this.patch({ countdown: countdown.seconds });
  }

  leaveRoom(): void {
    this.patch({
      screen: "lobby",
      lobbyMode: "tables",
      roomState: null,
      countdown: null,
      statusText: "",
      error: null,
      game: null,
    });
  }

  startGame(message: GameStartMessage): void {
    this.patch({
      screen: "game",
      countdown: null,
      statusText: "",
      error: null,
      game: {
        roomId: message.roomId,
        players: message.players,
        currentPlayerId: message.currentPlayerId,
        myHand: this.state.game?.myHand ?? [],
        selectedCardKeys: [],
        lastPlay: null,
        gameOver: null,
      },
    });
  }

  applyHand(message: HandMessage): void {
    const game = this.state.game;
    if (!game || game.roomId !== message.roomId) return;

    this.patch({
      game: {
        ...game,
        myHand: message.cards,
        selectedCardKeys: [],
      },
    });
  }

  applyPlayResult(message: PlayResultMessage): void {
    const game = this.state.game;
    if (!game || game.roomId !== message.roomId) return;

    const player = message.players.find((p) => p.playerId === message.playerId);
    const username = player?.username ?? "Player";

    this.patch({
      statusText: message.trickReset ? "New trick — free lead" : `${username} played`,
      game: {
        ...game,
        players: message.players,
        currentPlayerId: message.currentPlayerId,
        selectedCardKeys: [],
        lastPlay: message.trickReset
          ? null
          : {
              playerId: message.playerId,
              username,
              cards: message.cardsPlayed,
            },
      },
    });
  }

  applyPassResult(message: PassResultMessage): void {
    const game = this.state.game;
    if (!game || game.roomId !== message.roomId) return;

    const player = game.players.find((p) => p.playerId === message.playerId);
    const username = player?.username ?? "Player";

    this.patch({
      statusText: message.trickReset ? "Everyone passed — new trick" : `${username} passed`,
      game: {
        ...game,
        currentPlayerId: message.currentPlayerId,
        selectedCardKeys: [],
        lastPlay: message.trickReset ? null : game.lastPlay,
      },
    });
  }

  applyLeftDuringGame(message: LeftDuringGameMessage): void {
    const game = this.state.game;
    if (!game || game.roomId !== message.roomId) return;

    const action = message.autoPassed ? "passed (left)" : "left";
    this.patch({
      statusText: `${message.leftPlayerUsername} ${action}`,
      game: {
        ...game,
        players: message.players,
        currentPlayerId: message.currentPlayerId,
        selectedCardKeys: [],
        lastPlay: message.trickReset ? null : game.lastPlay,
      },
    });
  }

  applyGameOver(message: GameOverMessage): void {
    const game = this.state.game;
    if (!game || game.roomId !== message.roomId) return;

    const winner = game.players.find((p) => p.playerId === message.winnerId);
    const winnerUsername = winner?.username ?? "Player";

    this.patch({
      statusText: `${winnerUsername} wins!`,
      game: {
        ...game,
        currentPlayerId: message.winnerId,
        selectedCardKeys: [],
        gameOver: {
          winnerId: message.winnerId,
          winnerUsername,
        },
      },
    });
  }

  toggleCardSelection(card: { suit: string; rank: string }): void {
    const game = this.state.game;
    if (!game || game.gameOver) return;

    const key = cardKey(card as import("../types").Card);
    const selected = new Set(game.selectedCardKeys);
    if (selected.has(key)) selected.delete(key);
    else selected.add(key);

    this.patch({
      game: {
        ...game,
        selectedCardKeys: [...selected],
      },
    });
  }

  clearSelection(): void {
    const game = this.state.game;
    if (!game) return;
    this.patch({ game: { ...game, selectedCardKeys: [] } });
  }

  setError(message: string): void {
    this.patch({ error: message });
  }

  setStatus(message: string): void {
    this.patch({ statusText: message });
  }

  logout(): void {
    this.state = { ...initialState };
    this.emit();
  }
}

export const appStore = new AppStore();
