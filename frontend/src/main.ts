import "./style.css";
import { clearSession, loadSession, registerPlayer } from "./api/playerApi";
import { fetchRooms } from "./api/roomApi";
import { renderLogin } from "./views/loginView";
import { renderLobbyTable, renderLobbyTables } from "./views/lobbyView";
import { renderGame } from "./views/gameView";
import { appStore } from "./store/appStore";
import { GameSocket } from "./ws/GameSocket";
import { publishRoom, roomSummaryFromState, subscribeRooms } from "./ws/roomSync";
import { cardsFromKeys } from "./utils/cardUtils";
import type {
  CountdownMessage,
  ErrorMessage,
  GameOverMessage,
  GameStartMessage,
  HandMessage,
  InboundMessage,
  LeaveRoomMessage,
  LeftDuringGameMessage,
  PassResultMessage,
  PlayResultMessage,
  PlayerSession,
  RoomStateMessage,
} from "./types";

const root = document.querySelector<HTMLDivElement>("#app")!;
const socket = new GameSocket();
const ROOM_POLL_MS = 2000;
let roomPollTimer: number | null = null;
let roomPollingActive = false;

async function refreshRoomList(): Promise<void> {
  const state = appStore.getState();
  if (state.screen !== "lobby" || state.lobbyMode !== "tables") return;

  try {
    const rooms = await fetchRooms();
    appStore.setRooms(rooms);
  } catch {
    // ignore transient fetch errors while polling
  }
}

function syncRoomPolling(): void {
  const state = appStore.getState();
  const shouldPoll = state.screen === "lobby" && state.lobbyMode === "tables";

  if (shouldPoll && !roomPollingActive) {
    roomPollingActive = true;
    void refreshRoomList();
    roomPollTimer = window.setInterval(() => void refreshRoomList(), ROOM_POLL_MS);
    return;
  }

  if (!shouldPoll && roomPollingActive) {
    roomPollingActive = false;
    if (roomPollTimer !== null) {
      clearInterval(roomPollTimer);
      roomPollTimer = null;
    }
  }
}

function isRoomState(message: InboundMessage): message is RoomStateMessage {
  return message.type === "ROOM_STATE";
}

function isCountdown(message: InboundMessage): message is CountdownMessage {
  return message.type === "COUNTDOWN";
}

function isLeaveRoom(message: InboundMessage): message is LeaveRoomMessage {
  return message.type === "LEAVE_ROOM";
}

function isGameStart(message: InboundMessage): message is GameStartMessage {
  return message.type === "GAME_START";
}

function isHand(message: InboundMessage): message is HandMessage {
  return message.type === "HAND";
}

function isPlayResult(message: InboundMessage): message is PlayResultMessage {
  return message.type === "PLAY_RESULT";
}

function isPassResult(message: InboundMessage): message is PassResultMessage {
  return message.type === "PASS_RESULT";
}

function isGameOver(message: InboundMessage): message is GameOverMessage {
  return message.type === "GAME_OVER";
}

function isLeftDuringGame(message: InboundMessage): message is LeftDuringGameMessage {
  return message.type === "LEFT_DURING_GAME";
}

function isError(message: InboundMessage): message is ErrorMessage {
  return message.type === "ERROR";
}

function handleSocketMessage(message: InboundMessage): void {
  const state = appStore.getState();

  if (isError(message) && message.type !== "PONG") {
    appStore.setError(message.message);
    return;
  }

  if (isRoomState(message)) {
    const summary = roomSummaryFromState(message);
    appStore.upsertRoom(summary);
    publishRoom(message);

    if (state.screen === "game" || state.game?.gameOver) {
      appStore.enterRoom(message);
      return;
    }

    if (state.lobbyMode === "tables" && !state.roomState) {
      appStore.enterRoom(message);
    } else {
      appStore.updateRoomState(message);
    }
    return;
  }

  if (isCountdown(message)) {
    appStore.setCountdown(message);
    return;
  }

  if (isLeaveRoom(message)) {
    appStore.leaveRoom();
    void refreshRoomList();
    return;
  }

  if (isGameStart(message)) {
    appStore.startGame(message);
    return;
  }

  if (isHand(message)) {
    appStore.applyHand(message);
    return;
  }

  if (isPlayResult(message)) {
    appStore.applyPlayResult(message);
    return;
  }

  if (isPassResult(message)) {
    appStore.applyPassResult(message);
    return;
  }

  if (isGameOver(message)) {
    appStore.applyGameOver(message);
    return;
  }

  if (isLeftDuringGame(message)) {
    appStore.applyLeftDuringGame(message);
  }
}

function enterApp(session: PlayerSession): void {
  appStore.setPlayer(session);
  socket.disconnect();

  socket.connect(
    session,
    handleSocketMessage,
    () => {
      appStore.setWsConnected(true);
      appStore.setStatus("");
      syncRoomPolling();
      const joinRoomId = new URLSearchParams(window.location.search).get("join")?.trim();
      if (joinRoomId) {
        appStore.setStatus("Joining table...");
        socket.joinRoom(joinRoomId);
      }
    },
    () => appStore.setWsConnected(false),
  );
}

function signOut(): void {
  roomPollingActive = false;
  if (roomPollTimer !== null) {
    clearInterval(roomPollTimer);
    roomPollTimer = null;
  }
  socket.disconnect();
  clearSession();
  appStore.logout();
}

function leaveLobbyTable(): void {
  if (socket.isConnected()) {
    socket.leaveRoom();
  } else {
    appStore.leaveRoom();
  }
}

function leaveGame(): void {
  if (socket.isConnected()) {
    socket.leaveDuringGame();
  } else {
    appStore.leaveRoom();
  }
}

function render(): void {
  syncRoomPolling();
  const state = appStore.getState();

  switch (state.screen) {
    case "login":
      renderLogin(
        root,
        async (username) => {
          if (!username) {
            appStore.setError("Enter a name");
            return;
          }
          try {
            appStore.patch({ error: null });
            const session = await registerPlayer(username);
            enterApp(session);
          } catch (err) {
            appStore.setError(err instanceof Error ? err.message : "Login failed");
          }
        },
        state.error,
      );
      break;

    case "lobby":
      if (!state.player) {
        appStore.patch({ screen: "login" });
        return;
      }

      if (state.lobbyMode === "waiting" && state.roomState) {
        renderLobbyTable(
          root,
          state.player.username,
          state.player.id,
          state.roomState,
          state.countdown,
          state.error,
          state.wsConnected,
          () => socket.ready(),
          () => socket.notReady(),
          leaveLobbyTable,
        );
      } else {
        renderLobbyTables(
          root,
          state.player.username,
          state.rooms,
          state.statusText,
          state.error,
          state.wsConnected,
          () => {
            if (!state.wsConnected) {
              appStore.setError("Still connecting to server…");
              return;
            }
            appStore.setStatus("Creating table...");
            socket.createRoom();
          },
          (roomId) => {
            if (!state.wsConnected) {
              appStore.setError("Still connecting to server…");
              return;
            }
            appStore.setStatus("Joining table...");
            socket.joinRoom(roomId);
          },
          signOut,
        );
      }
      break;

    case "game":
      if (!state.player || !state.game) {
        appStore.patch({ screen: "login" });
        return;
      }
      renderGame(
        root,
        state.player.username,
        state.player.id,
        state.game,
        state.statusText,
        state.error,
        state.wsConnected,
        (suit, rank) => appStore.toggleCardSelection({ suit, rank }),
        () => {
          const game = appStore.getState().game;
          if (!game) return;
          const cards = cardsFromKeys(game.myHand, game.selectedCardKeys);
          if (cards.length === 0) {
            appStore.setError("Select at least one card");
            return;
          }
          appStore.patch({ error: null });
          socket.play(cards);
        },
        () => {
          appStore.patch({ error: null });
          socket.pass();
        },
        () => appStore.clearSelection(),
        leaveGame,
      );
      break;
  }
}

function bootstrap(): void {
  appStore.subscribe(render);
  subscribeRooms((summary) => appStore.upsertRoom(summary));

  const session = loadSession();
  if (session?.local) {
    clearSession();
    appStore.patch({ screen: "login" });
  } else if (session) {
    enterApp(session);
  } else {
    appStore.patch({ screen: "login" });
  }
}

bootstrap();
