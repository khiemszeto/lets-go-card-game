package com.gameplatform.service;

import com.gameplatform.game.Room;
import com.gameplatform.game.RoomManager;
import com.gameplatform.game.model.Card;
import com.gameplatform.game.model.Rank;
import com.gameplatform.game.model.Suit;
import com.gameplatform.game.rules.TienLenValidator;
import com.gameplatform.websocket.RoomNotifier;
import com.gameplatform.websocket.dto.common.CardDto;
import com.gameplatform.websocket.dto.common.ErrorMessageDto;
import com.gameplatform.websocket.dto.common.RoomPlayerDto;
import com.gameplatform.websocket.dto.outbound.game.*;
import com.gameplatform.websocket.dto.outbound.lobby.LeftRoomMessageDto;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class GameService {
    private final RoomManager roomManager;
    private final RoomNotifier roomNotifier;
    private final TienLenValidator validator;
    private LobbyService lobbyService;
    private TaskScheduler taskScheduler;

    public GameService(RoomManager roomManager, RoomNotifier roomNotifier
            , TienLenValidator validator, LobbyService lobbyService, TaskScheduler taskScheduler) {
        this.roomManager = roomManager;
        this.roomNotifier = roomNotifier;
        this.validator = validator;
        this.lobbyService = lobbyService;
        this.taskScheduler = taskScheduler;
    }

    public Object play(Long playerId, List<CardDto> cardDtos) {
        Room room = roomManager.getRoomForPlayer(playerId);

        if (room == null) return error("You are not in a room");
        if (!room.isStarted()) return error("The game has not started yet");
        if (!playerId.equals(room.getCurrentTurnPlayerId())) return error("It is not your turn");
        if (cardDtos == null || cardDtos.isEmpty()) return error("You must play a card");

        List<Card> playedCards;

        try {
            playedCards = mapToCards(cardDtos);
        } catch (IllegalArgumentException e) {
            return error("Invalid card");
        }

        List<Card> hand = room.getHand(playerId);
        if (hand == null || hand.isEmpty()) return error("You do not have any cards");
        if (!playerOwnsCards(hand, playedCards)) return error("You do not own these cards");

        boolean freeLead = room.getLastPlayCards() == null || room.getLastPlayCards().isEmpty();

        if (freeLead && !room.isFirstTrickPlayed()){
            if (hand.contains(new Card(Suit.SPADE, Rank.THREE))
                    && !playedCards.contains(new Card(Suit.SPADE, Rank.THREE))) {
                return error("First play must include a three of spades");
            }
            if (!playedCards.contains(validator.getLowestCard(hand))) {
                return error("First play must include the lowest card");
            }
        }

        // Validate the play
        if (!validator.canPlay(playedCards, room.getLastPlayCards())) {
            return error("Invalid play");
        }

        // Validating step passes
        for (Card c : playedCards) {
            hand.remove(c);
        }

        room.setLastPlayCards(playedCards);
        room.setFirstTrickPlayed(true);
        room.setConsecutivePasses(0);
        room.setLastPlayPlayerId(playerId);

        // Empty hand = game over
        if (hand.isEmpty()) {
            PlayResultMessageDto playResult = new PlayResultMessageDto();
            playResult.setRoomId(room.getId().toString());
            playResult.setPlayerId(playerId);
            playResult.setCardsPlayed(cardDtos);
            playResult.setCurrentPlayerId(room.getCurrentTurnPlayerId());
            playResult.setPlayers(buildPlayerSummaries(room));
            playResult.setTrickReset(false);

            roomNotifier.sendToRoom(room, playResult);

            GameOverMessageDto gameOver = new GameOverMessageDto();
            gameOver.setRoomId(room.getId().toString());
            gameOver.setWinnerId(playerId);


            // use this to settle the score aftermatch
            Map<Long, List<Card>> losers = room.getAllHandsForScoring();
            losers.remove(playerId);




            roomNotifier.sendToRoom(room, gameOver);
            room.resetToWaiting();

            UUID roomId = room.getId();
            taskScheduler.schedule(() -> {
                Room r = roomManager.getRoom(roomId);
                if (r == null || !r.isWaiting()) return; // left or already rematching
                roomNotifier.sendToRoom(r, lobbyService.buildRoomState(r));
            }, Instant.now().plusSeconds(5));
            return gameOver;
        }

        // Advance turn
        room.setCurrentTurnPlayerId(room.nextPlayerId());

        // Public the PLAY_RESULT
        PlayResultMessageDto playResult = new PlayResultMessageDto();
        playResult.setRoomId(room.getId().toString());
        playResult.setPlayerId(playerId);
        playResult.setCardsPlayed(cardDtos);
        playResult.setCurrentPlayerId(room.getCurrentTurnPlayerId());
        playResult.setPlayers(buildPlayerSummaries(room));
        playResult.setTrickReset(false);

        roomNotifier.sendToRoom(room, playResult);


        // Private update hand to the player
        HandMessageDto handMessageDto = new HandMessageDto();
        handMessageDto.setRoomId(room.getId().toString());
        handMessageDto.setCards(mapToCardDtos(hand));
        roomNotifier.sendToPlayer(playerId, handMessageDto);

        return playResult;
    }

    public Object pass(Long playerId) {
        Room room = roomManager.getRoomForPlayer(playerId);

        if (room == null) return error("You are not in a room");
        if (!room.isStarted()) return error("The game has not started yet");
        if (!playerId.equals(room.getCurrentTurnPlayerId())) return error("It is not your turn");


        boolean freeLead = room.getLastPlayCards() == null || room.getLastPlayCards().isEmpty();
        if (freeLead) return error("You cannot pass on a free lead");

        room.setConsecutivePasses(room.getConsecutivePasses() + 1);

        int activePlayers = countActivePlayers(room);
        boolean trickReset = room.getConsecutivePasses() >= activePlayers - 1;

        if (trickReset) {
            room.setLastPlayCards(null);
            room.setConsecutivePasses(0);
            room.setCurrentTurnPlayerId(room.getLastPlayPlayerId());
        } else {
            room.setCurrentTurnPlayerId(room.nextPlayerId());
        }

        PassResultMessageDto passResult = new PassResultMessageDto();
        passResult.setRoomId(room.getId().toString());
        passResult.setPlayerId(playerId);
        passResult.setCurrentPlayerId(room.getCurrentTurnPlayerId());
        passResult.setTrickReset(trickReset);

        roomNotifier.sendToRoom(room, passResult);
        return passResult;

    }

    private ErrorMessageDto error(String message) {
        ErrorMessageDto dto = new ErrorMessageDto();
        dto.setMessage(message);
        return dto;
    }

    private List<Card> mapToCards(List<CardDto> cardDtos) {
        List<Card> cards = new ArrayList<>();
        for (CardDto c: cardDtos) {
            cards.add(new Card(Suit.valueOf(c.getSuit()), Rank.valueOf(c.getRank())));
        }

        return cards;
    }

    private List<CardDto> mapToCardDtos(List<Card> cards) {
        List<CardDto> cardDtos = new ArrayList<>();

        for (Card c: cards) {
            CardDto cardDto = new CardDto();
            cardDto.setRank(c.getRank().name());
            cardDto.setSuit(c.getSuit().name());
            cardDtos.add(cardDto);
        }

        return cardDtos;
    }

    private boolean playerOwnsCards(List<Card> hand, List<Card> played) {
        List<Card> copy = new ArrayList<>(hand);
        for (Card c: played) {
            if (!copy.remove(c)) return false;
        }

        return true;
    }

    private int countActivePlayers(Room room) {
        int count = 0;
        for (List<Card> hand : room.getHands().values()) {
            if (hand != null && !hand.isEmpty()) count++;
        }
        return count;
    }

    private Long findTheOnePlayerActiveWithCards(Room room) {
        for (Long id: room.getSeatOrder()) {
            if (!room.getMapOfPlayers().containsKey(id)) continue;
            List<Card> hand = room.getHand(id);
            if (hand != null && !hand.isEmpty()) return id;
        }

        return null;
    }

    private List<RoomPlayerDto> buildPlayerSummaries(Room room) {
        List<RoomPlayerDto> players = new ArrayList<>();

        for (Map.Entry<Long, List<Card>> entry : room.getHands().entrySet()) {
            Long id = entry.getKey();
            List<Card> hand = entry.getValue();

            RoomPlayerDto dto = new RoomPlayerDto();
            dto.setPlayerId(id);
            dto.setUsername(room.getMapOfPlayers().get(id));
            dto.setSeat(room.getSeatOrder().indexOf(id) + 1);
            dto.setNumberOfCards(hand == null ? 0 : hand.size());
            dto.setReady(room.getWhoIsReady().contains(id));
            players.add(dto);
        }

        return players;
    }

    public Object leaveRoomWhilePlay(Long playerId, Room room) {
        String username = room.getMapOfPlayers().get(playerId);
        boolean wasTheirTurn = playerId.equals((room.getCurrentTurnPlayerId()));
        boolean isFreeLead = room.getLastPlayCards() == null || room.getLastPlayCards().isEmpty();

        room.archiveHandForScoring(playerId);
        roomManager.leaveRoom(playerId);

        int activePlayers = countActivePlayers(room);

        if (activePlayers <= 1) {
            Long winnerId = findTheOnePlayerActiveWithCards(room);

            if (winnerId != null) {
                GameOverMessageDto gameOver = new GameOverMessageDto();
                gameOver.setRoomId(room.getId().toString());
                gameOver.setWinnerId(winnerId);

                // use this to settle the score aftermatch
                Map<Long, List<Card>> losers = room.getAllHandsForScoring();
                losers.remove(winnerId);

                roomNotifier.sendToRoom(room, gameOver);

                LeftRoomMessageDto leftRoomMessageDto = new LeftRoomMessageDto();
                leftRoomMessageDto.setRoomId(room.getId().toString());
                leftRoomMessageDto.setType("LEAVE_ROOM");

                room.resetToWaiting();

                UUID roomId = room.getId();
                taskScheduler.schedule(() -> {
                    Room r = roomManager.getRoom(roomId);
                    if (r == null || !r.isWaiting()) return; // left or already rematching
                    roomNotifier.sendToRoom(r, lobbyService.buildRoomState(r));
                }, Instant.now().plusSeconds(5));

                return leftRoomMessageDto;
            } else {
                LeftRoomMessageDto leftRoomMessageDto = new LeftRoomMessageDto();
                leftRoomMessageDto.setRoomId(room.getId().toString());
                leftRoomMessageDto.setType("LEAVE_ROOM");
                return leftRoomMessageDto;
            }
        } else {
            // case 1: leave when I have the free lead
            if (wasTheirTurn && isFreeLead) {
                Long freeLeadInheritId = room.nextPlayerId();
                room.setCurrentTurnPlayerId(freeLeadInheritId);

                LeftRoomMessageDto leftRoomMessageDto = new LeftRoomMessageDto();
                leftRoomMessageDto.setRoomId(room.getId().toString());
                leftRoomMessageDto.setType("LEAVE_ROOM");

                LeftDuringGameMessageDto leftDuringGameMessageDto = new LeftDuringGameMessageDto();
                leftDuringGameMessageDto.setRoomId(room.getId().toString());
                leftDuringGameMessageDto.setLeftPlayerId(playerId);
                leftDuringGameMessageDto.setLeftPlayerUsername(username);
                leftDuringGameMessageDto.setCurrentPlayerId(freeLeadInheritId);
                leftDuringGameMessageDto.setPlayers(buildPlayerSummaries(room));
                leftDuringGameMessageDto.setTrickReset(false);
                leftDuringGameMessageDto.setAutoPassed(false);
                roomNotifier.sendToRoom(room, leftDuringGameMessageDto);

                return leftRoomMessageDto;

                // is their turn only
            } else if (wasTheirTurn) {
                room.setConsecutivePasses(room.getConsecutivePasses() + 1);

                boolean trickReset = room.getConsecutivePasses() >= activePlayers - 1;


                if (trickReset) {
                    Long newTrickPlayerId = room.getLastPlayPlayerId();
                    room.setLastPlayCards(null);
                    room.setConsecutivePasses(0);

                    if (newTrickPlayerId != null && !room.getMapOfPlayers().containsKey(newTrickPlayerId)) {
                        newTrickPlayerId = room.nextPlayerId();
                    }

                    room.setCurrentTurnPlayerId(newTrickPlayerId);

                    LeftDuringGameMessageDto leftDuringGameMessageDto = new LeftDuringGameMessageDto();
                    leftDuringGameMessageDto.setRoomId(room.getId().toString());
                    leftDuringGameMessageDto.setLeftPlayerId(playerId);
                    leftDuringGameMessageDto.setLeftPlayerUsername(username);
                    leftDuringGameMessageDto.setCurrentPlayerId(newTrickPlayerId);
                    leftDuringGameMessageDto.setPlayers(buildPlayerSummaries(room));
                    leftDuringGameMessageDto.setTrickReset(true);
                    leftDuringGameMessageDto.setAutoPassed(true);
                    roomNotifier.sendToRoom(room, leftDuringGameMessageDto);

                } else {
                    Long nextPlayerId = room.nextPlayerId();
                    room.setCurrentTurnPlayerId(nextPlayerId);

                    LeftDuringGameMessageDto leftDuringGameMessageDto = new LeftDuringGameMessageDto();
                    leftDuringGameMessageDto.setRoomId(room.getId().toString());
                    leftDuringGameMessageDto.setLeftPlayerId(playerId);
                    leftDuringGameMessageDto.setLeftPlayerUsername(username);
                    leftDuringGameMessageDto.setCurrentPlayerId(nextPlayerId);
                    leftDuringGameMessageDto.setPlayers(buildPlayerSummaries(room));
                    leftDuringGameMessageDto.setTrickReset(false);
                    leftDuringGameMessageDto.setAutoPassed(true);
                    roomNotifier.sendToRoom(room, leftDuringGameMessageDto);

                }

                LeftRoomMessageDto leftRoomMessageDto = new LeftRoomMessageDto();
                leftRoomMessageDto.setRoomId(room.getId().toString());
                leftRoomMessageDto.setType("LEAVE_ROOM");

                return leftRoomMessageDto;

                // not their turn
            } else {
                Long current = room.getCurrentTurnPlayerId();
                if (current != null && !room.getMapOfPlayers().containsKey(current)) {
                    room.setCurrentTurnPlayerId(room.nextPlayerId());
                }

                LeftDuringGameMessageDto leftDuringGameMessageDto = new LeftDuringGameMessageDto();
                leftDuringGameMessageDto.setRoomId(room.getId().toString());
                leftDuringGameMessageDto.setLeftPlayerId(playerId);
                leftDuringGameMessageDto.setLeftPlayerUsername(username);
                leftDuringGameMessageDto.setCurrentPlayerId(room.getCurrentTurnPlayerId());
                leftDuringGameMessageDto.setPlayers(buildPlayerSummaries(room));
                leftDuringGameMessageDto.setTrickReset(false);
                leftDuringGameMessageDto.setAutoPassed(false);
                roomNotifier.sendToRoom(room, leftDuringGameMessageDto);


                LeftRoomMessageDto leftRoomMessageDto = new LeftRoomMessageDto();
                leftRoomMessageDto.setRoomId(room.getId().toString());
                leftRoomMessageDto.setType("LEAVE_ROOM");

                return leftRoomMessageDto;
            }
        }




    }



}
