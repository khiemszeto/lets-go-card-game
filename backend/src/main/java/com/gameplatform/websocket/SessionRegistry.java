package com.gameplatform.websocket;

import com.gameplatform.entity.Player;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Memory? of the connection
 *
 * Tracks the WebSocket sessions that are currently connected.
 * Map out who is connected
 *
 * Sessions are added when a connection opens (server - player connected) and removed when it closes (disconnected or network drop), so the
 * registry always reflects who is reachable right now.
 *
 * */
@Component
public class SessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Map<Long, WebSocketSession> sessionPlayers = new ConcurrentHashMap<>();

    public void add(WebSocketSession session, Long playerId) throws IOException {
        // check if player is already connected
        if (sessionPlayers.containsKey(playerId)) {
            WebSocketSession oldSession = sessionPlayers.get(playerId);
            sessions.remove(oldSession.getId());
            oldSession.close();
        }

        sessions.put(session.getId(), session);
        sessionPlayers.put(playerId, session);
    }

    public void remove(WebSocketSession session) {
        Long playerId = (Long) session.getAttributes().get("playerId");

        if (playerId != null && session == sessionPlayers.get(playerId)) {
            sessionPlayers.remove(playerId);
        }

        sessions.remove(session.getId());
    }

    public int count() {
        return sessions.size();
    }

    public Collection<WebSocketSession> all() {
        return sessions.values();
    }

    /**
     * Message every connected session. A session that fails to accept
     * the message is dropped rather than failing the whole broadcast.
     *
     * e.g. tell everyone new player joined
     * e.g. Alice play 7A -> broadcast to everyone that Alice played 7A
     * */
    public void broadcast(String payload) {
        TextMessage message = new TextMessage(payload);

        for (WebSocketSession session : sessions.values()) {
            send(session, message);
        }
    }

    public void send(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) {
            remove(session);
            return;
        }

        // WebSocketSession is not safe for concurrent senders
        // like, If Two or more broadcasting happen at once, it would interleave frames on the same socket and corrupt the stream
        // which is why we need to sync on a particular section, and using ConcurrentHashMap for Map (instead of ordinary HashMap)
        synchronized (session) {
            try {
                session.sendMessage(message);
            } catch (IOException e) {
                remove(session);
            }
        }
    }
}
