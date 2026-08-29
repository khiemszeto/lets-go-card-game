import type { RoomStateMessage, RoomSummary } from "../types";
import { bindLeaveButton, renderPageShell } from "./pageShell";

function statusLabel(status: RoomSummary["status"]): string {
  switch (status) {
    case "WAITING":
      return "Open";
    case "COUNTING_DOWN":
      return "Starting";
    case "STARTED":
      return "In Game";
  }
}

function shortId(roomId: string): string {
  return roomId.slice(0, 8).toUpperCase();
}

function banners(statusText: string | null, error: string | null, wsConnected: boolean): string {
  const parts: string[] = [];
  if (!wsConnected) {
    parts.push(`<p class="status-banner">Connecting to server…</p>`);
  }
  if (statusText) parts.push(`<p class="status-banner">${escapeHtml(statusText)}</p>`);
  if (error) parts.push(`<p class="error-banner">${escapeHtml(error)}</p>`);
  return parts.join("");
}

/** Page 2a — table hall */
export function renderLobbyTables(
  root: HTMLElement,
  username: string,
  rooms: RoomSummary[],
  statusText: string | null,
  error: string | null,
  wsConnected: boolean,
  onCreateRoom: () => void,
  onJoinRoom: (roomId: string) => void,
  onLeave: () => void,
): void {
  const roomCards =
    rooms.length > 0
      ? rooms
          .map((room) => {
            const disabled = !room.joinable || room.status === "STARTED";
            return `
              <button
                class="room-box ${disabled ? "room-box--disabled" : ""}"
                data-room-id="${room.roomId}"
                type="button"
                ${disabled ? "disabled" : ""}
              >
                <span class="room-box__badge">${statusLabel(room.status)}</span>
                <strong class="room-box__title">Table ${shortId(room.roomId)}</strong>
                <span class="room-box__host">Host: ${escapeHtml(room.hostUsername)}</span>
                <span class="room-box__meta">${room.playerCount}/${room.maxPlayers} players · ${room.readyPlayers} ready</span>
              </button>
            `;
          })
          .join("")
      : `<p class="hall-empty">No open tables yet. Create one, or open a second tab to test join.</p>`;

  const body = `
    <div class="lobby-toolbar">
      <button id="create-room-btn" class="btn btn-primary" type="button">+ Create Table</button>
    </div>
    <div class="room-grid">${roomCards}</div>
    <div class="join-by-id">
      <label>
        Or join by table ID
        <div class="join-by-id__row">
          <input id="join-room-input" type="text" placeholder="Paste table UUID" />
          <button id="join-room-btn" class="btn btn-secondary" type="button">Join</button>
        </div>
      </label>
    </div>
  `;

  root.innerHTML = renderPageShell(
    "Game Lobby",
    "Pick a table",
    username,
    "Sign out",
    body,
    banners(statusText, error, wsConnected),
  );

  bindLeaveButton(root, onLeave);
  root.querySelector("#create-room-btn")?.addEventListener("click", onCreateRoom);

  root.querySelectorAll<HTMLButtonElement>(".room-box:not(.room-box--disabled)").forEach((btn) => {
    btn.addEventListener("click", () => {
      const roomId = btn.dataset.roomId;
      if (roomId) onJoinRoom(roomId);
    });
  });

  const joinInput = root.querySelector<HTMLInputElement>("#join-room-input")!;
  const submitJoin = () => {
    const roomId = joinInput.value.trim();
    if (roomId) onJoinRoom(roomId);
  };
  root.querySelector("#join-room-btn")?.addEventListener("click", submitJoin);
  joinInput.addEventListener("keydown", (e) => {
    if (e.key === "Enter") submitJoin();
  });
}

const SEAT_POSITIONS = ["seat-top", "seat-right", "seat-bottom", "seat-left"];

/** Page 2b — inside a table, waiting to start */
export function renderLobbyTable(
  root: HTMLElement,
  username: string,
  playerId: number,
  roomState: RoomStateMessage,
  countdown: number | null,
  error: string | null,
  wsConnected: boolean,
  onReady: () => void,
  onNotReady: () => void,
  onLeave: () => void,
): void {
  const me = roomState.players.find((p) => p.playerId === playerId);
  const isReady = me?.ready ?? false;

  const seats = roomState.players
    .map((player, index) => {
      const pos = SEAT_POSITIONS[index] ?? "seat-top";
      const isMe = player.playerId === playerId;
      return `
        <div class="room-seat ${pos} ${isMe ? "room-seat--me" : ""}">
          <div class="room-seat__avatar">${escapeHtml(player.username.slice(0, 1).toUpperCase())}</div>
          <div class="room-seat__info">
            <strong>${escapeHtml(player.username)}${isMe ? " (you)" : ""}</strong>
            <span>Seat ${player.seat}</span>
            <span class="ready-pill ${player.ready ? "ready-pill--on" : ""}">
              ${player.ready ? "Ready" : "Not ready"}
            </span>
          </div>
        </div>
      `;
    })
    .join("");

  const body = `
    <p class="table-id-hint">Table ID: <code>${escapeHtml(roomState.roomId)}</code></p>
    <div class="room-table-wrap">
      <div class="room-table">
        ${countdown !== null ? `<div class="countdown-overlay">Starting in ${countdown}</div>` : ""}
        ${seats}
        <div class="room-center">
          <p class="room-center__label">Ready up</p>
          ${
            isReady
              ? `<button id="not-ready-btn" class="btn btn-secondary btn-lg" type="button">Not Ready</button>`
              : `<button id="ready-btn" class="btn btn-primary btn-lg" type="button">Ready</button>`
          }
        </div>
      </div>
    </div>
  `;

  root.innerHTML = renderPageShell(
    `Table ${shortId(roomState.roomId)}`,
    "Waiting room",
    username,
    "Leave",
    body,
    banners(null, error, wsConnected),
  );

  bindLeaveButton(root, onLeave);
  root.querySelector("#ready-btn")?.addEventListener("click", onReady);
  root.querySelector("#not-ready-btn")?.addEventListener("click", onNotReady);
}

function escapeHtml(text: string): string {
  return text
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;");
}
