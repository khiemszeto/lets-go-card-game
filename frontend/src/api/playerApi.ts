import type { PlayerSession } from "../types";

const SESSION_KEY = "cardgame.player";

function randomSuffix(): string {
  return Math.random().toString(36).slice(2, 8);
}

/** Local login only — no REST. WebSocket needs a registered player id on the backend. */
export function loginLocal(username: string): PlayerSession {
  const trimmed = username.trim();
  if (!trimmed) throw new Error("Enter a name");

  const session: PlayerSession = {
    id: Date.now() * 1000 + Math.floor(Math.random() * 1000),
    username: trimmed,
    local: true,
  };

  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}

export function loadSession(): PlayerSession | null {
  const raw = sessionStorage.getItem(SESSION_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as PlayerSession;
  } catch {
    return null;
  }
}

export function clearSession(): void {
  sessionStorage.removeItem(SESSION_KEY);
}

/**
 * Optional: call when you want WebSocket auth against the real backend.
 * Not used by the login page while REST is disabled.
 */
export async function registerPlayer(username: string): Promise<PlayerSession> {
  const suffix = randomSuffix();
  const res = await fetch("/api/players", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      username: username.trim(),
      password: "password1",
      email: `${username.trim().toLowerCase()}_${suffix}@play.local`,
      birthDate: "2000-01-01",
    }),
  });

  if (!res.ok) {
    const err = await res.json().catch(() => ({ message: "Registration failed" }));
    throw new Error(err.message ?? "Registration failed");
  }

  const data = await res.json();
  const session: PlayerSession = { id: data.id, username: data.username };
  sessionStorage.setItem(SESSION_KEY, JSON.stringify(session));
  return session;
}
