package com.gameplatform.websocket;

import com.gameplatform.game.Room;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import tools.jackson.databind.ObjectMapper;

@Component
public class RoomNotifier {
    private final SessionRegistry sessionRegistry;
    private final ObjectMapper objectMapper;

    public RoomNotifier(SessionRegistry sessionRegistry, ObjectMapper objectMapper) {
        this.sessionRegistry = sessionRegistry;
        this.objectMapper = objectMapper;
    }

    public void sendToRoom(Room room, Object messageDto) {
        String json = objectMapper.writeValueAsString((messageDto));
        TextMessage textMessage = new TextMessage(json);

        for (Long playerId: room.getMapOfPlayers().keySet()) {
            sessionRegistry.sendToPlayer(playerId, textMessage);
        }
    }

    public void sendToPlayer(Long playerId, Object messageDto) {
        String json = objectMapper.writeValueAsString((messageDto));
        sessionRegistry.sendToPlayer(playerId, new TextMessage(json));
    }

}
