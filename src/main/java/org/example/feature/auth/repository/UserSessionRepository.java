package org.example.feature.auth.repository;

import org.example.feature.auth.model.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findByTokenHashAndIsRevokedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE UserSession s SET s.isRevoked = true WHERE s.user.id = :userId")
    void revokeAllSessionsByUserId(@Param("userId") Long userId);
}

