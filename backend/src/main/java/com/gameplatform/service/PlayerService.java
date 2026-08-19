package com.gameplatform.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.gameplatform.annotation.TrackExecutionTime;
import com.gameplatform.dto.CreatePlayerRequestDto;
import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.entity.Player;
import com.gameplatform.exception.DuplicateResourceException;
import com.gameplatform.exception.ResourceNotFoundException;
import com.gameplatform.repository.PlayerRepository;

@Service
public class PlayerService {
    PlayerRepository playerRepository;


    public PlayerService(PlayerRepository playerRepository) {
        this.playerRepository = playerRepository;
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
        Player player = playerRepository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Player with id " + id + " not found")
        );

        return mapToDTO(player);
    }

    public List<CreatePlayerResponseDto> getTop10Players() {
        List<Player> top10 = playerRepository.findTop10ByOrderByBalanceDesc();

        return top10.stream().map(this::mapToDTO).toList();
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
        player.setPassword(requestDto.getPassword());
        player.setBirthDate(requestDto.getBirthDate());
        player.setEmail(requestDto.getEmail());
        player.setCreatedAt(LocalDateTime.now());
        player.setUpdatedAt(LocalDateTime.now());
        player.setDeleted(false);
        player.setBalance(0L);

        return player;

    }

    private boolean emailExist(String email) {
        return     playerRepository.existsByEmail(email);
    }

    private boolean userExist(Player player) {
        return playerRepository.existsByUsername(player.getUsername());
    }
}
