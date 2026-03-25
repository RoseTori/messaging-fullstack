package com.example.messaging.moderation.application;

import com.example.messaging.common.application.ForbiddenOperationException;
import com.example.messaging.common.domain.UuidV7;
import com.example.messaging.moderation.domain.UserBlock;
import com.example.messaging.moderation.infrastructure.UserBlockRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final UserBlockRepository userBlockRepository;

    @Transactional
    public void block(UUID blockerId, UUID blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("User cannot block themselves");
        }
        if (userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            return;
        }
        UserBlock block = new UserBlock();
        block.setId(UuidV7.randomUuid());
        block.setBlockerId(blockerId);
        block.setBlockedId(blockedId);
        userBlockRepository.save(block);
    }

    @Transactional
    public void unblock(UUID blockerId, UUID blockedId) {
        userBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public void assertCanMessage(UUID senderId, UUID otherUserId) {
        if (userBlockRepository.existsByBlockerIdAndBlockedId(senderId, otherUserId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(otherUserId, senderId)) {
            throw new ForbiddenOperationException("Messaging is blocked between these users");
        }
    }
}
