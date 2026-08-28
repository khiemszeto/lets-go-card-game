import type { Card, InboundMessage, PlayerSession } from "../types";

type MessageHandler = (message: InboundMessage) => void;

export class GameSocket {
  private ws: WebSocket | null = null;
  private handler: MessageHandler | null = null;

  connect(session: PlayerSession, onMessage: MessageHandler, onOpen?: () => void, onClose?: () => void): void {
    this.handler = onMessage;
    const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
    const url = `${protocol}//${window.location.host}/ws?playerId=${session.id}`;
    this.ws = new WebSocket(url);

    this.ws.onopen = () => onOpen?.();

    this.ws.onclose = () => onClose?.();

    this.ws.onmessage = (event) => {
      if (!this.handler || typeof event.data !== "string") return;
      if (event.data.startsWith("welcome") || event.data.startsWith("online")) return;

      try {
        const message = JSON.parse(event.data) as InboundMessage;
        this.handler(message);
      } catch {
        // ignore non-json frames
      }
    };
  }

  send(payload: Record<string, unknown>): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;
    this.ws.send(JSON.stringify(payload));
  }

  createRoom(): void {
    this.send({ type: "CREATE_ROOM" });
  }

  joinRoom(roomId: string): void {
    this.send({ type: "JOIN_ROOM", roomId });
  }

  ready(): void {
    this.send({ type: "READY" });
  }

  notReady(): void {
    this.send({ type: "NOT_READY" });
  }

  leaveRoom(): void {
    this.send({ type: "LEAVE_ROOM" });
  }

  leaveDuringGame(): void {
    this.send({ type: "LEAVE_DURING_GAME" });
  }

  play(cards: Card[]): void {
    this.send({ type: "PLAY", cards });
  }

  pass(): void {
    this.send({ type: "PASS" });
  }

  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN;
  }

  disconnect(): void {
    this.ws?.close();
    this.ws = null;
    this.handler = null;
  }
}
