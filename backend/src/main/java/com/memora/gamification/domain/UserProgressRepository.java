package com.memora.gamification.domain;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProgressRepository extends JpaRepository<UserProgress, UUID> {

    Optional<UserProgress> findByUserId(UUID userId);
}
