import { apiFetch } from './apiClient'
import type { RoomSummary } from '../types/game'


export async function fetchRooms(): Promise<RoomSummary[]> {
    const res = await apiFetch('/api/rooms')
    return res.json()
}