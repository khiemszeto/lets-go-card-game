package com.gameplatform.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.gameplatform.game.model.Card;
import com.gameplatform.websocket.dto.outbound.game.BalanceChangeDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.gameplatform.annotation.TrackExecutionTime;
import com.gameplatform.dto.CreatePlayerRequestDto;
import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.entity.Player;
import com.gameplatform.exception.DuplicateResourceException;
import com.gameplatform.exception.ResourceNotFoundException;
import com.gameplatform.repository.PlayerRepository;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthPlayerService {
    private PlayerRepository playerRepository;
    private PasswordEncoder passwordEncoder;

    public AuthPlayerService(PlayerRepository playerRepository,PasswordEncoder passwordEncoder) {
        this.playerRepository = playerRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @TrackExecutionTime(
            operation = "Create New Player",
            warnAfter = 1500L
    )
    public CreatePlayerResponseDto createPlayer(CreatePlayerRequestDto requestDto) {
        Player player = mapToPlayer(requestDto);

        if (emailExist(player.getEmail())) {
            throw new DuplicateResourceException("This email cannot be used. Please try another.");
        }

        if (userExist(player)) {
            throw new DuplicateResourceException("This username cannot be used. Please try another.");
        }

        Player playerResponse = playerRepository.save(player);

        return mapToDTO(playerResponse);
    }

    @TrackExecutionTime(
            operation = "Soft Delete Player",
            warnAfter = 1500L
    )
    public void deletePlayerSoftly(Long id) {
        Player player = playerRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Player with id " + id + " not found")
        );

        player.setDeleted(true);
        playerRepository.save(player);
    }


    @TrackExecutionTime(
            operation = "Get All Players",
            warnAfter = 1500L
    )
    public List<CreatePlayerResponseDto> getAllPlayers() {
        List<Player> players = playerRepository.findAll();

        return players.stream()
                .map(this::mapToDTO).toList();

    }

    public List<Player> fetchByPage() {
        // pagination
        Pageable pageable = PageRequest.of(0, 10);

        Page<Player> playerList =  playerRepository.findAll(pageable);

        return playerList.toList();
    }


    @TrackExecutionTime(
            operation = "Get A Player By Id",
            warnAfter = 1000L
    )
    public CreatePlayerResponseDto getPlayer(Long id) {
        Player player = playerRepository.findByIdAndDeletedIsFalse(id).orElseThrow(
                () -> new ResourceNotFoundException("Player with id " + id + " not found")
        );

        return mapToDTO(player);
    }

    public List<CreatePlayerResponseDto> getTop10Players() {
        List<Player> top10 = playerRepository.findTop10ByOrderByBalanceDesc();

        return top10.stream().map(this::mapToDTO).toList();
    }


    @Transactional
    public List<BalanceChangeDto> settleBalanceByRemainingCards
            (Long winnerId, Map<Long, List<Card>> remainingHands) {

        long pot = 0L;
        Map<Long, Long> deltas = new LinkedHashMap<>();

        for (Map.Entry<Long, List<Card>> entry : remainingHands.entrySet()) {
            Long loserId = entry.getKey();
            if (loserId.equals(winnerId)) continue;

            List<Card> loserCards = entry.getValue();
            long penalty = loserCards.size() * 10;
            deltas.put(loserId, -penalty);
            pot += penalty;
        }

        deltas.put(winnerId, pot);

        List<BalanceChangeDto> result = new ArrayList<>();

        for (Map.Entry<Long, Long> delta : deltas.entrySet()) {
            Long playerId = delta.getKey();
            Long deltaAmount = delta.getValue();

            Player player = playerRepository.findById(playerId).orElseThrow(
                    () -> new ResourceNotFoundException("Player with id " + playerId + " not found")
            );


            long newBalance = player.getBalance() + deltaAmount;
            player.setBalance(newBalance);
            player.setUpdatedAt(LocalDateTime.now());

            BalanceChangeDto balanceChangeDto = new BalanceChangeDto();
            balanceChangeDto.setPlayerName(player.getUsername());
            balanceChangeDto.setDelta(deltaAmount);
            balanceChangeDto.setNewBalance(newBalance);
            result.add(balanceChangeDto);
        }
        return result;

    }

    private CreatePlayerResponseDto mapToDTO(Player playerResponse) {
        CreatePlayerResponseDto createPlayerResponseDto = new CreatePlayerResponseDto();

        createPlayerResponseDto.setId(playerResponse.getId());
        createPlayerResponseDto.setUsername(playerResponse.getUsername());
        createPlayerResponseDto.setBalance(playerResponse.getBalance());
        createPlayerResponseDto.setCreatedAt(playerResponse.getCreatedAt());

        return createPlayerResponseDto;
    }

    private Player mapToPlayer(CreatePlayerRequestDto requestDto) {
        Player player = new Player();

        player.setUsername(requestDto.getUsername());

        String endecodedPassword = passwordEncoder.encode(requestDto.getPassword());
        player.setPassword(endecodedPassword);

        player.setEmail(requestDto.getEmail());
        player.setCreatedAt(LocalDateTime.now());
        player.setUpdatedAt(LocalDateTime.now());
        player.setDeleted(false);
        player.setBalance(2000L);

        return player;

    }

    private boolean emailExist(String email) {
        return     playerRepository.existsByEmail(email);
    }

    private boolean userExist(Player player) {
        return playerRepository.existsByUsername(player.getUsername());
    }
}
