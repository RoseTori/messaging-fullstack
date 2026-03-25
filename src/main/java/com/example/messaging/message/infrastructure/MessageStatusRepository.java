package com.example.messaging.message.infrastructure;

import com.example.messaging.message.domain.MessageDeliveryStatus;
import com.example.messaging.message.domain.MessageStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageStatusRepository extends JpaRepository<MessageStatus, UUID> {
    List<MessageStatus> findByMessageId(UUID messageId);
    Optional<MessageStatus> findByMessageIdAndUserId(UUID messageId, UUID userId);
    @Query("""
            select ms from MessageStatus ms
            where ms.userId = :userId
              and ms.status = :status
            """)
    List<MessageStatus> findByUserIdAndStatus(@Param("userId") UUID userId, @Param("status") MessageDeliveryStatus status);

    @Modifying
    @Query("""
            update MessageStatus ms
               set ms.status = :toStatus,
                   ms.statusAt = current_timestamp
             where ms.userId = :userId
               and ms.status = :fromStatus
            """)
    int bulkUpdateStatusForUser(@Param("userId") UUID userId,
                                @Param("fromStatus") MessageDeliveryStatus fromStatus,
                                @Param("toStatus") MessageDeliveryStatus toStatus);
}
