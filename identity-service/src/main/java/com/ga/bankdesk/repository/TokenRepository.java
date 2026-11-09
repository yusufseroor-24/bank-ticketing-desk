package com.ga.bankdesk.repository;

import com.ga.bankdesk.model.Token;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TokenRepository extends JpaRepository<Token, Long> {
    Optional<Token> findByToken(String token);

    //return number of rows effected: 1 if got the token, 0 if already used
    @Modifying(flushAutomatically = true)
    @Query("UPDATE Token t SET t.used = true WHERE t.id = :tokenId AND t.used = false")
    int markTokenUsedIfUnused(@Param("tokenId") Long tokenId);
}
