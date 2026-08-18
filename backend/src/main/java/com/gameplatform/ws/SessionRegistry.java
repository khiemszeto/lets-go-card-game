package com.gameplatform.ws;

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

    public void add(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    public void remove(WebSocketSession session) {
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
