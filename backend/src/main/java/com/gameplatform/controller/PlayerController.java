package com.gameplatform.controller;


import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameplatform.dto.CreatePlayerRequestDto;
import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.service.AuthPlayerService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/players")
public class PlayerController {
    AuthPlayerService authPlayerService;


    public PlayerController(AuthPlayerService authPlayerService) {
        this.authPlayerService = authPlayerService;
    }

    @PostMapping
    public ResponseEntity<CreatePlayerResponseDto> createPlayer(
            @Valid @RequestBody CreatePlayerRequestDto createPlayerRequestDto) {

        CreatePlayerResponseDto createPlayerResponseDto = authPlayerService.createPlayer(createPlayerRequestDto);

        return ResponseEntity.ok(createPlayerResponseDto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> deletePlayerSoftly(@PathVariable Long id) {
        authPlayerService.deletePlayerSoftly(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping
    public ResponseEntity<List<CreatePlayerResponseDto>> getAllPlayers() {
        List<CreatePlayerResponseDto> playerList = authPlayerService.getAllPlayers();

        return ResponseEntity.ok(playerList);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CreatePlayerResponseDto> getPlayer(@PathVariable Long id) {
        CreatePlayerResponseDto player = authPlayerService.getPlayer(id);

        return ResponseEntity.ok(player);
    }

    @GetMapping("/top")
    public ResponseEntity<List<CreatePlayerResponseDto>> getTopPlayers() {
        List<CreatePlayerResponseDto> topPlayers = authPlayerService.getTop10Players();


        return ResponseEntity.ok(topPlayers);
    }
}
