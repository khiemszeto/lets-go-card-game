package com.gameplatform.service;

import com.gameplatform.entity.GameHistory;
import com.gameplatform.entity.Player;
import com.gameplatform.game.Room;
import com.gameplatform.game.model.Card;
import com.gameplatform.repository.GameHistoryRepository;
import com.gameplatform.repository.PlayerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;

@Service
public class GameHistoryService {
    private final GameHistoryRepository gameHistoryRepository;
    private PlayerRepository playerRepository;
    private ObjectMapper objectMapper;

    public GameHistoryService(PlayerRepository playerRepository,
                              ObjectMapper objectMapper,
                              GameHistoryRepository gameHistoryRepository) {
        this.playerRepository = playerRepository;
        this.objectMapper = objectMapper;
        this.gameHistoryRepository = gameHistoryRepository;
    }

    @Transactional
    public void recordGame(Room room, Long winnerId) {
        if (room.getStartingHands() == null || room.getStartingHands().isEmpty()) return;

        GameHistory gameHistory = new GameHistory();
        gameHistory.setPlayedAt(LocalDateTime.now());
        gameHistory.setInitialHands(
                objectMapper.writeValueAsString(serializeHands(room.getStartingHands()))
        );

        gameHistory.setFirstPlayerId(room.getHistoryFirstPlayerId());

        Set<Long> playerIds = new HashSet<>(room.getStartingHands().keySet());
        gameHistory.setPlayers(new HashSet<>(playerRepository.findAllById(playerIds)));


        if (winnerId != null) {
            Player winner = playerRepository.findById(winnerId).orElseThrow(
                    () -> new IllegalStateException("Player with id " + winnerId + " not found")
            );
            gameHistory.setWinner(winner);
        }

        gameHistory.setMoves(objectMapper.writeValueAsString(room.getMoveLog()));

        gameHistory.setSeatOrder(objectMapper.writeValueAsString(room.getStartingPlayOrder()));

        gameHistoryRepository.save(gameHistory);
    }


    // map Map<Long, List<Card>> hands to Map<String, List<Map<String,String>>>
    private Map<String, List<Map<String,String>>> serializeHands(Map<Long, List<Card>> hands) {
        Map<String, List<Map<String,String>>> output = new LinkedHashMap<>();

        for (Map.Entry<Long, List<Card>> entry : hands.entrySet()) {
            List<Map<String,String>> cards = new ArrayList<>();

            for (Card card : entry.getValue()) {
                Map<String, String> oneCard = new LinkedHashMap<>();
                oneCard.put("rank", card.getRank().name());
                oneCard.put("suit", card.getSuit().name());
                cards.add(oneCard);
            }

            output.put(entry.getKey().toString(), cards);
        }

        return output;



    }




}
