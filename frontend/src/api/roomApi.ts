import type { RoomSummary } from "../types";

export async function fetchRooms(): Promise<RoomSummary[]> {
  const res = await fetch("/api/rooms");
  if (!res.ok) {
    throw new Error("Failed to load tables");
  }
  return (await res.json()) as RoomSummary[];
}
