package com.gameplatform.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.gameplatform.dto.RoomSummaryDto;
import com.gameplatform.service.LobbyService;

@RestController
@RequestMapping("/api/rooms")
public class LobbyController {

    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping
    public ResponseEntity<List<RoomSummaryDto>> listRooms() {
        return ResponseEntity.ok(lobbyService.listRoomSummaries());
    }
}
