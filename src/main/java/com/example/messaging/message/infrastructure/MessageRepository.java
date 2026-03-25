package com.example.messaging.message.infrastructure;

import com.example.messaging.message.domain.Message;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, UUID> {
    List<Message> findByChatRoomIdOrderByCreatedAtDesc(UUID chatRoomId, Pageable pageable);
    List<Message> findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(UUID chatRoomId, LocalDateTime before, Pageable pageable);
    Optional<Message> findByChatRoomIdAndSenderIdAndClientMessageId(UUID chatRoomId, UUID senderId, String clientMessageId);
}
