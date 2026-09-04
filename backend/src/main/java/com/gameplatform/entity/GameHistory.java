package com.gameplatform.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter
@Setter
public class GameHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, columnDefinition = "TEXT") //
    private String moves;

    @Column(nullable = false, columnDefinition = "TEXT")//
    private String initialHands;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String seatOrder;

    @Column(nullable = false)//
    private Long firstPlayerId;

    @Column(nullable = false)//
    private LocalDateTime playedAt;

    @ManyToOne(fetch = FetchType.LAZY)//
    @JoinColumn(name = "winner_id")
    private Player winner;

    @ManyToMany//
    @JoinTable(
            name="player_game_history",
            joinColumns = @JoinColumn(name = "game_history_id"),
            inverseJoinColumns = @JoinColumn(name="player_id")
    )
    private Set<Player> players = new HashSet<>();



}
