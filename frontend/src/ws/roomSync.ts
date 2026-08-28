import type { RoomStateMessage, RoomSummary } from "../types";

const CHANNEL_NAME = "cardgame-room-sync";

let channel: BroadcastChannel | null = null;

function getChannel(): BroadcastChannel | null {
  if (typeof BroadcastChannel === "undefined") return null;
  if (!channel) channel = new BroadcastChannel(CHANNEL_NAME);
  return channel;
}

export function roomSummaryFromState(state: RoomStateMessage): RoomSummary {
  const host = state.players[0];
  return {
    roomId: state.roomId,
    hostUsername: host?.username ?? "Unknown",
    playerCount: state.players.length,
    maxPlayers: 4,
    readyPlayers: state.readyPlayers,
    status: "WAITING",
    joinable: state.players.length < 4,
  };
}

export function publishRoom(state: RoomStateMessage): void {
  getChannel()?.postMessage({ type: "ROOM_UPSERT", summary: roomSummaryFromState(state) });
}

export function subscribeRooms(onUpsert: (summary: RoomSummary) => void): () => void {
  const ch = getChannel();
  if (!ch) return () => undefined;

  const handler = (event: MessageEvent) => {
    const data = event.data as { type?: string; summary?: RoomSummary };
    if (data?.type === "ROOM_UPSERT" && data.summary) onUpsert(data.summary);
  };

  ch.addEventListener("message", handler);
  return () => ch.removeEventListener("message", handler);
}

export function upsertRoomList(rooms: RoomSummary[], incoming: RoomSummary): RoomSummary[] {
  const idx = rooms.findIndex((r) => r.roomId === incoming.roomId);
  if (idx === -1) return [...rooms, incoming];
  const next = [...rooms];
  next[idx] = { ...next[idx], ...incoming };
  return next;
}
