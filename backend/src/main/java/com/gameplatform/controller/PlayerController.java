package com.gameplatform.controller;


import com.gameplatform.dto.CreatePlayerRequestDto;
import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.service.PlayerService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.http.HttpResponse;
import java.util.List;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    PlayerService playerService;

    public PlayerController(PlayerService playerService) {
        this.playerService = playerService;
    }

    @PostMapping
    public ResponseEntity<CreatePlayerResponseDto> createPlayer(
            @Valid @RequestBody CreatePlayerRequestDto createPlayerRequestDto) {

        CreatePlayerResponseDto createPlayerResponseDto = playerService.createPlayer(createPlayerRequestDto);

        return ResponseEntity.ok(createPlayerResponseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePlayerSoftly(@PathVariable Long id) {
        playerService.deletePlayerSoftly(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<List<CreatePlayerResponseDto>> getAllPlayers() {
        List<CreatePlayerResponseDto> playerList = playerService.getAllPlayers();

        return ResponseEntity.ok(playerList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreatePlayerResponseDto> getPlayer(@PathVariable Long id) {
        CreatePlayerResponseDto player = playerService.getPlayer(id);

        return ResponseEntity.ok(player);
    }


}
