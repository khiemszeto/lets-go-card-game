package com.gameplatform.repository;

import com.gameplatform.dto.CreatePlayerResponseDto;
import com.gameplatform.entity.Player;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PlayerRepository extends JpaRepository<Player, Long> {

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    List<Player> findTop10ByOrderByBalanceDesc();

    @Query(value = """
            SELECT *
            FROM player
            ORDER BY balance    DESC 
            LIMIT 10;
            """, nativeQuery = true)
    List<Player> findBottom10ByBalance();

    @Query(value = """
            SELECT *
            FROM player
            WHERE email = :email
            LIMIT 10;
            """, nativeQuery = true)
    List<Player> findByEmail(@Param("email") String email);
}
