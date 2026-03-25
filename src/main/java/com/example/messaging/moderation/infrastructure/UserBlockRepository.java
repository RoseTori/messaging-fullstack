package com.example.messaging.moderation.infrastructure;

import com.example.messaging.moderation.domain.UserBlock;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {
    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);
}
