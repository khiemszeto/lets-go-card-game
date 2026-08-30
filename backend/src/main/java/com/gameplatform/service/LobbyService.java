package com.gameplatform.service;


import com.gameplatform.dto.RoomSummaryDto;
import com.gameplatform.game.Room;
import com.gameplatform.game.RoomManager;
import com.gameplatform.game.model.Card;
import com.gameplatform.game.model.Deck;
import com.gameplatform.game.model.Rank;
import com.gameplatform.game.model.Suit;
import com.gameplatform.game.rules.TienLenValidator;
import com.gameplatform.websocket.RoomNotifier;
import com.gameplatform.websocket.dto.common.CardDto;
import com.gameplatform.websocket.dto.common.ErrorMessageDto;
import com.gameplatform.websocket.dto.common.RoomPlayerDto;
import com.gameplatform.websocket.dto.outbound.game.GameOverMessageDto;
import com.gameplatform.websocket.dto.outbound.game.GameStartMessageDto;
import com.gameplatform.websocket.dto.outbound.game.HandMessageDto;
import com.gameplatform.websocket.dto.outbound.lobby.CountdownMessageDto;
import com.gameplatform.websocket.dto.outbound.lobby.LeftRoomMessageDto;
import com.gameplatform.websocket.dto.outbound.lobby.RoomStateMessageDto;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * This class should handle the lobby of users
 * where user can pick a game, and play in a room together?
 * */

@Service
public class LobbyService {

    private final RoomNotifier roomNotifier;
    private RoomManager roomManager;
    private ConcurrentHashMap<UUID, List<ScheduledFuture<?>>> countdownRooms = new ConcurrentHashMap<>();
    private TaskScheduler taskScheduler;
    private TienLenValidator tienLenValidator;

    public LobbyService(RoomManager roomManager,
                        TaskScheduler taskScheduler,
                        RoomNotifier roomNotifier,
                        TienLenValidator tienLenValidator) {
        this.roomManager = roomManager;
        this.taskScheduler = taskScheduler;
        this.roomNotifier = roomNotifier;
        this.tienLenValidator = tienLenValidator;
    }

    public Object createRoom(Long playerId, String username) {
        UUID roomID = roomManager.createRoom(playerId, username);

        if (roomID == null) {
           ErrorMessageDto error =  new ErrorMessageDto();
           error.setMessage("Cannot create room, already in room");
           return error;
        }

        return buildRoomState(roomManager.getRoom(roomID));
    }

    public Object joinRoom(Long playerId, String username, UUID roomId) {
        RoomManager.JoinStatus status = roomManager.joinRoom(playerId, username, roomId);

        switch (status) {
            case SUCCESS -> {
                return buildRoomState(roomManager.getRoom(roomId));
            }

            case ROOM_NOT_FOUND -> {
                return error("Room not found");
            }

            case ROOM_FULL -> {
                return error("Room is full");
            }

            case ALREADY_IN_ROOM -> {
                return error("Already in room");
            }

            default -> throw new IllegalStateException("Unexpected value: " + status);
        }
    }

    public Object playerReady(Long playerId) {
        Room room = roomManager.getRoomForPlayer(playerId);
        if (room == null) {
            return error ("You are not in a room");
        }
        room.setReady(playerId);

        if (room.isWaiting() && room.isReadyToStart()) {
            if (room.startCountingDown()) {
                List<ScheduledFuture<?>> jobs = new ArrayList<>();

                // tick at 10s .. 5s 4s 3s 2s 1s
                for (int sec = 10; sec >= 1; sec--) {
                    int secondsLeft = sec;
                    long delaySecond = 10 -sec;

                    ScheduledFuture<?> tick = taskScheduler.schedule(() ->
                            sendCountdownTick(room.getId(), secondsLeft)
                            , Instant.now().plusSeconds(delaySecond)
                            );

                    jobs.add(tick);
                }

                // finish at 10s
                ScheduledFuture<?> finish = taskScheduler.schedule(
                        () -> onCountdownFinished(room.getId()),
                        Instant.now().plusSeconds(10)
                );
                jobs.add(finish);

                countdownRooms.put(room.getId(), jobs);
            }
        }

        return buildRoomState(room);
    }




    public Object playerNotReady(Long playerId) {
        Room room = roomManager.getRoomForPlayer(playerId);
        if (room == null) {
            return error("You are not in a room");
        }
        room.setNotReady(playerId);

        cancelCountdownIfNeeded(room);

        return buildRoomState(room);
    }

    public Room getRoomByUUID(UUID roomUUID) {
        return roomManager.getRoom(roomUUID);
    }

    public Room getRoomForPlayer(Long playerId) {
        return roomManager.getRoomForPlayer(playerId);
    }

    public List<RoomSummaryDto> listRoomSummaries() {
        List<RoomSummaryDto> summaries = new ArrayList<>();

        for (Room room : roomManager.listRooms()) {
            if (room.isEmpty()) continue;

            RoomSummaryDto dto = new RoomSummaryDto();
            dto.setRoomId(room.getId().toString());
            dto.setMaxPlayers(4);
            dto.setPlayerCount(room.getMapOfPlayers().size());
            dto.setReadyPlayers(room.getWhoIsReady().size());

            List<Long> seatOrder = room.getSeatOrder();
            Long hostId = seatOrder.isEmpty() ? null : seatOrder.get(0);
            dto.setHostUsername(hostId != null ? room.getMapOfPlayers().get(hostId) : "Unknown");

            if (room.isStarted()) {
                dto.setStatus("STARTED");
                dto.setJoinable(false);
            } else if (room.isCountingDown()) {
                dto.setStatus("COUNTING_DOWN");
                dto.setJoinable(false);
            } else {
                dto.setStatus("WAITING");
                dto.setJoinable(!room.isFull());
            }

            summaries.add(dto);
        }

        return summaries;
    }

    public Object leaveRoom(Long playerId) {
        Room room = roomManager.getRoomForPlayer(playerId);

        if (room == null) {
            return error("You are not in a room");
        }

        return leaveRoomWhileWaiting(playerId, room);
    }



    private Object leaveRoomWhileWaiting(Long playerId, Room room) {
        roomManager.leaveRoom(playerId);
        cancelCountdownIfNeeded(room);

        LeftRoomMessageDto leftRoomMessageDto = new LeftRoomMessageDto();
        leftRoomMessageDto.setType("LEAVE_ROOM");
        leftRoomMessageDto.setRoomId(room.getId().toString());

        return leftRoomMessageDto;

    }

    public RoomStateMessageDto buildRoomState(Room room) {
        RoomStateMessageDto roomStateMessageDto = new RoomStateMessageDto();

        roomStateMessageDto.setRoomId(room.getId().toString());
        roomStateMessageDto.setReadyPlayers(room.getWhoIsReady().size());

        List<RoomPlayerDto> players = new ArrayList<>();

        for (Map.Entry<Long, String> entry : room.getMapOfPlayers().entrySet()) {
            RoomPlayerDto playerDto = new RoomPlayerDto();

            playerDto.setPlayerId(entry.getKey());
            playerDto.setUsername(entry.getValue());
            playerDto.setReady(room.getWhoIsReady().contains(entry.getKey()));
            playerDto.setSeat(room.getSeatOrder().indexOf(entry.getKey()) + 1);

            players.add(playerDto);
        }

        roomStateMessageDto.setPlayers(players);

        return roomStateMessageDto;
    }

    private ErrorMessageDto error(String message) {
        ErrorMessageDto dto = new ErrorMessageDto();
        dto.setMessage(message);
        return dto;
    }

    private void onCountdownFinished(UUID roomId) {
        countdownRooms.remove(roomId);

        Room room = roomManager.getRoom(roomId);

        if (room == null) return;
        if (!room.isCountingDown()) return;
        if (!room.isReadyToStart()) {
            room.cancelCountingDown();
            return;
        }

        // GAME PLAY STARTS HERE
        room.markStarted();
        room.setFirstTrickPlayed(false);

        room.clearHands();

        Deck deck = new Deck();
        deck.shuffle();

        List<Long> players = room.getWhoIsReady().stream().toList();

        List<RoomPlayerDto> playersDto = new ArrayList<>();

        for (Long playerId: players) {
            List<Card> hand = deck.dealCards();

            if (hand.contains(new Card(Suit.SPADES, Rank.THREE))) room.setCurrentTurnPlayerId(playerId);
            room.setHand(playerId, hand);


            RoomPlayerDto roomPlayerDto = new RoomPlayerDto();
            roomPlayerDto.setUsername(room.getMapOfPlayers().get(playerId));
            roomPlayerDto.setPlayerId(playerId);
            roomPlayerDto.setNumberOfCards(hand.size());
            roomPlayerDto.setSeat(room.getSeatOrder().indexOf(playerId) + 1);
            playersDto.add(roomPlayerDto);
        }

        if (room.getCurrentTurnPlayerId() == null) {
            Long theFirstPlayer = null;
            Card lowestCard = null;
            for (Map.Entry<Long,List<Card>> entry: room.getHands().entrySet()) {
                Card curCard = tienLenValidator.getLowestCard(entry.getValue());
                if (lowestCard == null) {
                    lowestCard = curCard;
                    theFirstPlayer = entry.getKey();
                }
                else {
                    if (tienLenValidator.compareCard(curCard, lowestCard) < 0) {
                        lowestCard = curCard;
                        theFirstPlayer = entry.getKey();
                    }
                }
            }

            room.setCurrentTurnPlayerId(theFirstPlayer);
        }

        GameStartMessageDto gameStartMessageDto = new GameStartMessageDto();
        gameStartMessageDto.setRoomId(roomId.toString());
        gameStartMessageDto.setPlayers(playersDto);
        gameStartMessageDto.setCurrentPlayerId(room.getCurrentTurnPlayerId());
        roomNotifier.sendToRoom(room, gameStartMessageDto); // public message

        for (Long playerId: players) {
            List<Card> hand = room.getHand(playerId);



            HandMessageDto handMessageDto = new HandMessageDto();
            handMessageDto.setRoomId(roomId.toString());

            List<CardDto> cardDtos = new ArrayList<>();
            for (Card card: hand) {
                CardDto cardDto = new CardDto();
                cardDto.setRank(card.getRank().name());
                cardDto.setSuit(card.getSuit().name());
                cardDtos.add(cardDto);
            }

            handMessageDto.setCards(cardDtos);
            roomNotifier.sendToPlayer(playerId, handMessageDto); // private message

        }

        for (Long playerId: players) {
            List<Card> hand = room.getHand(playerId);

            if (hasQuadOfTwos(hand)) {
                GameOverMessageDto gameOver = new GameOverMessageDto();
                gameOver.setRoomId(roomId.toString());
                gameOver.setWinnerId(playerId);
                roomNotifier.sendToRoom(room, gameOver);
                room.resetToWaiting();

                taskScheduler.schedule(() -> {
                    Room r = roomManager.getRoom(roomId);
                    if (r == null || !r.isWaiting()) return; // left or already rematching
                    roomNotifier.sendToRoom(r, buildRoomState(r));
                }, Instant.now().plusSeconds(5));

                return;
            }
        }
    }

    private void cancelCountdownIfNeeded(Room room) {
        if (!room.isCountingDown()) return;
        if (room.isReadyToStart()) return;

        List<ScheduledFuture<?>> jobs = countdownRooms.remove(room.getId());
        if (jobs != null) {
            for (ScheduledFuture<?> job : jobs) {
                job.cancel(false);
            }
        }

        room.cancelCountingDown();
    }

    private void sendCountdownTick(UUID roomId, int secondsLeft) {
        Room room = roomManager.getRoom(roomId);
        if (room == null || !room.isCountingDown()) return;

        CountdownMessageDto dto = new CountdownMessageDto();

        dto.setType("COUNTDOWN");
        dto.setRoomId(roomId.toString());
        dto.setSeconds(secondsLeft);

        roomNotifier.sendToRoom(room, dto);
    }

    private boolean hasQuadOfTwos(List<Card> hand) {
        if (hand == null) return false;
        return hand.stream().filter(c -> c.getRank() == Rank.TWO).count() == 4;
    }

}
