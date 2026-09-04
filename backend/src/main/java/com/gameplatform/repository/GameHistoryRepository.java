package com.gameplatform.repository;

import com.gameplatform.entity.GameHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GameHistoryRepository extends JpaRepository<GameHistory, Long> {
}
