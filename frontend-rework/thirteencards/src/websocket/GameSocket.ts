import { getAccessToken } from "../auth/authStorage"

type MessageHandler = (data: unknown) => void;

export class GameSocket {
    private websocket: WebSocket | null = null;
    private onMessage: MessageHandler | null = null;

    connect(
        onMessage: MessageHandler,
        onOpen?: () => void,
        onClose?: () => void
    ) {
        const token = getAccessToken();
        if(!token) throw new Error("Not logged in");

        this.onMessage = onMessage;

        const protocol = window.location.protocol === "https:" ? "wss" : "ws";

        const url = `${protocol}://${window.location.host}/ws?token=${encodeURIComponent(token)}`;

        this.websocket = new WebSocket(url);

        this.websocket.onopen = () => onOpen?.();
        this.websocket.onclose = () => onClose?.();

        this.websocket.onmessage = (event) => {
            if (!this.onMessage || typeof event.data !== "string") return;

            if (event.data.startsWith("welcome") || event.data.startsWith("online")) {
                console.log("[WS]" + event.data);
                return;
            }

            // Real game events are JSON. Parse and pass to your UI handler. Unknown formats → warn, don’t crash.
            try {
                this.onMessage(JSON.parse(event.data));
            } catch {
                console.warn("[WS] Non-JSON message received: ", event.data)
            }
        }
    }

    private send(payload: Record<string, unknown>) {
        if (!this.websocket || this.websocket.readyState !== WebSocket.OPEN) {
            console.warn("[WS] not connected, cannot send", payload.type);
            return;
        }
        this.websocket.send(JSON.stringify(payload));
    }

    public createRoom() {this.send({type: "CREATE_ROOM"})}
    public joinRoom(roomId: string) { this.send({ type: 'JOIN_ROOM', roomId }) }
    public leaveRoom() { this.send({ type: 'LEAVE_ROOM' }) }
    public leaveDuringGame() { this.send({ type: 'LEAVE_DURING_GAME' }) }
    public ready() { this.send({ type: 'READY' }) }
    public notReady() { this.send({ type: 'NOT_READY' }) }
    public play(cards: { suit: string; rank: string }[]) {
        this.send({ type: 'PLAY', cards })
    }
    public pass() { this.send({ type: 'PASS' }) }

    public disconnect() {
        this.websocket?.close()
        this.websocket = null
        this.onMessage = null
    }

    public isConnected() {
        return this.websocket?.readyState === WebSocket.OPEN
    }

}