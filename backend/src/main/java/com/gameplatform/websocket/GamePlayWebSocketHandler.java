package com.gameplatform.websocket;

import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.exception.ResourceNotFoundException;
import com.gameplatform.game.Room;
import com.gameplatform.service.GameService;
import com.gameplatform.service.LobbyService;
import com.gameplatform.service.PlayerService;
import com.gameplatform.websocket.dto.common.ErrorMessageDto;
import com.gameplatform.websocket.dto.inbound.ClientMessageDto;
import com.gameplatform.websocket.dto.outbound.lobby.LeftRoomMessageDto;
import com.gameplatform.websocket.dto.outbound.lobby.RoomStateMessageDto;
import com.gameplatform.websocket.dto.outbound.system.PongMessageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.util.UUID;

/**
 * Behavior? during connection
 *
 * Handles the lifecycle of a player's realtime connection.
 *
 * For now this only tracks who are connected and echoes messages back to everyone.
 * Basically to check the end to end connection.
 *
 * Probably will build Game room and Lobby routing on top of this.
 * */
@Component
public class GamePlayWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(GamePlayWebSocketHandler.class);

    private final SessionRegistry sessionRegistry;

    private PlayerService playerService;

    private ObjectMapper objectMapper;

    private LobbyService lobbyService;

    private GameService gameService;

    private RoomNotifier roomNotifier;

    public GamePlayWebSocketHandler(SessionRegistry sessionRegistry,
                                    PlayerService playerService,
                                    LobbyService lobbyService,
                                    GameService gameService,
                                    ObjectMapper objectMapper,
                                    RoomNotifier roomNotifier
    ) {
        this.sessionRegistry = sessionRegistry;
        this.playerService = playerService;
        this.objectMapper = objectMapper;
        this.lobbyService = lobbyService;
        this.roomNotifier = roomNotifier;
        this.gameService = gameService;
    }


    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        URI uri = session.getUri();
        if (uri == null) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing URI"));
            return;
        }

        MultiValueMap<String, String> params =
                UriComponentsBuilder.fromUri(uri).build().getQueryParams();

        String playerIdString = params.getFirst("playerId");

        if (playerIdString == null || playerIdString.isBlank()) {
            session.close(CloseStatus.BAD_DATA.withReason("Missing playerId"));
            return;
        }

        Long playerId;
        try {
            playerId = Long.parseLong(playerIdString.trim());
        }catch (NumberFormatException e) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid playerId"));
            return;
        }

        CreatePlayerResponseDto player;
        try {
            player = playerService.getPlayer(playerId);
        } catch (ResourceNotFoundException e) {
            session.close(CloseStatus.BAD_DATA.withReason("Invalid playerId"));
            return;
        }

        session.getAttributes().put("playerId", playerId);
        session.getAttributes().put("username", player.getUsername());

        // update map to remember this new connected section
        sessionRegistry.add(session, playerId);
        log.info("WebSocket connected: player {} ({}) — {} online",
                player.getUsername(), player.getId(), sessionRegistry.count());
        sessionRegistry.send(session, new TextMessage("welcome " + session.getId()));
        sessionRegistry.broadcast("online " + sessionRegistry.count());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("WebSocket message from {}: {}", session.getId(), message.getPayload());
        if (message.getPayload().isBlank()) { return;}

        ClientMessageDto data;

        try {
            data  = objectMapper.readValue(message.getPayload(), ClientMessageDto.class);
        }catch (JacksonException e) {
            ErrorMessageDto errorMessageDto = new ErrorMessageDto();
            errorMessageDto.setMessage("Json invalid bro !!");
            sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
            return;
        }

        if (data.getType() == null || data.getType().isBlank()) {
            ErrorMessageDto errorMessageDto = new ErrorMessageDto();
            errorMessageDto.setMessage("Json invalid bro !!");
            sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
            return;
        }

        switch (data.getType()) {
            case "PING" ->  {
                PongMessageDto pongMessageDto = new PongMessageDto();
                pongMessageDto.setType("PONG");
                sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(pongMessageDto)));
            }

            case "CREATE_ROOM" -> {
                // server create a new room and send back the room id
                Long playerId = (Long) session.getAttributes().get("playerId");
                String username = session.getAttributes().get("username").toString();

                Object messageDto = lobbyService.createRoom(playerId, username);

                sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
            }

            case "JOIN_ROOM" -> {
                Long playerId = (Long) session.getAttributes().get("playerId");
                String username = session.getAttributes().get("username").toString();
                String roomId = data.getRoomId();

                if (roomId == null || roomId.isBlank()) {
                    ErrorMessageDto errorMessageDto = new ErrorMessageDto();
                    errorMessageDto.setMessage("Json invalid bro !!");
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
                    return;
                }

                UUID roomUUID;

                try {
                    roomUUID = UUID.fromString(roomId);
                } catch (IllegalArgumentException e) {
                    ErrorMessageDto errorMessageDto = new ErrorMessageDto();
                    errorMessageDto.setMessage("Json invalid bro !!");
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
                    return;
                }

                // expect RoomStateMessageDt in happy path, BUT ErrorMessageDto in case of error
                Object messageDto = lobbyService.joinRoom(playerId, username, roomUUID);
                if (messageDto instanceof RoomStateMessageDto roomStateMessageDto) {
                    Room room = lobbyService.getRoomByUUID(roomUUID);
                    roomNotifier.sendToRoom(room, roomStateMessageDto);
                } else {
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
                }

            }

            case "READY" -> {
                // client click ready botton
                Long playerId = (Long) session.getAttributes().get("playerId");

                // expect RoomStateMessageDt in happy path, BUT ErrorMessageDto in case of error
                Object messageDto = lobbyService.playerReady(playerId);

                if (messageDto instanceof RoomStateMessageDto roomStateMessageDto) {
                    String roomId = roomStateMessageDto.getRoomId();
                    Room room = lobbyService.getRoomByUUID(UUID.fromString(roomId));
                    roomNotifier.sendToRoom(room, roomStateMessageDto);
                } else {
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
                }
            }

            case "NOT_READY" -> {
                Long playerId = (Long) session.getAttributes().get("playerId");

                // expect RoomStateMessageDt in happy path, BUT ErrorMessageDto in case of error
                Object messageDto =  lobbyService.playerNotReady(playerId);

                if (messageDto instanceof RoomStateMessageDto roomStateMessageDto) {
                    String roomId = roomStateMessageDto.getRoomId();
                    Room room = lobbyService.getRoomByUUID(UUID.fromString(roomId));
                    roomNotifier.sendToRoom(room, roomStateMessageDto);
                } else {
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
                }
            }

            case "LEAVE_ROOM" -> {
                Long playerId = (Long) session.getAttributes().get("playerId");

                Object messageDto = lobbyService.leaveRoom(playerId);

                if (messageDto instanceof LeftRoomMessageDto leftRoomMessageDto) {
                    String roomId = leftRoomMessageDto.getRoomId();
                    Room room = lobbyService.getRoomByUUID(UUID.fromString(roomId));

                    if (room != null && !room.isStarted()) {
                        RoomStateMessageDto roomStateMessageDto = lobbyService.buildRoomState(room);
                        roomNotifier.sendToRoom(room, roomStateMessageDto);
                    }
                }
                sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
            }

            case "LEAVE_DURING_GAME" -> {
                Long playerId = (Long) session.getAttributes().get("playerId");

                Room room = lobbyService.getRoomForPlayer(playerId);

                if (room == null) {
                    ErrorMessageDto errorMessageDto = new ErrorMessageDto();
                    errorMessageDto.setMessage("You are not in a room");
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
                    return;
                }

                if (!room.isStarted()) {
                    ErrorMessageDto errorMessageDto = new ErrorMessageDto();
                    errorMessageDto.setMessage("Room is not started yet !!");
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
                    return;
                }

                Object messageDto = gameService.leaveRoomWhilePlay(playerId, room);
                sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));

            }

            case "PLAY" -> {
                Long playerId = (Long) session.getAttributes().get("playerId");

                Object messageDto = gameService.play(playerId, data.getCards());

                if (messageDto instanceof ErrorMessageDto) {
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
                }

            }

            case "PASS" -> {
                Long playerId = (Long) session.getAttributes().get("playerId");

                Object messageDto = gameService.pass(playerId);

                if (messageDto instanceof ErrorMessageDto) {
                    sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(messageDto)));
                }
            }


            default -> {
                ErrorMessageDto errorMessageDto = new ErrorMessageDto();
                errorMessageDto.setMessage("UNKNOWN TYPE bro !!!");
                sessionRegistry.send(session, new TextMessage(objectMapper.writeValueAsString(errorMessageDto)));
            }
        }
    }

    // this does not handle the case where laptop lid or a dropped Wi-Fi
    // where socket may still sit there looking open before we verify it failure
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        // remove recorded session from mapping

        Long playerId = (Long) session.getAttributes().get("playerId");
        Room room = lobbyService.getRoomForPlayer(playerId);

        // not in a room
        if (room == null) {
            sessionRegistry.remove(session);
            // in a room but not started yet
        } else if (!room.isStarted()) {
            // expect LeftRoomMessageDto in happy path, BUT ErrorMessageDto in case of error
            Object messageDto = lobbyService.leaveRoom(playerId);

            if (messageDto instanceof LeftRoomMessageDto leftRoomMessageDto) {
                RoomStateMessageDto roomStateMessageDto = lobbyService.buildRoomState(room);
                roomNotifier.sendToRoom(room, roomStateMessageDto);
                sessionRegistry.remove(session);
            }
        } else if (room.isStarted()) {
            gameService.leaveRoomWhilePlay(playerId, room);
            sessionRegistry.remove(session);
        }
    }
}
