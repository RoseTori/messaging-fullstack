package com.example.messaging.chat.infrastructure;

import com.example.messaging.chat.domain.ChatMember;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatMemberRepository extends JpaRepository<ChatMember, UUID> {
    List<ChatMember> findByChatRoomIdAndLeftAtIsNull(UUID chatRoomId);
    List<ChatMember> findByUserIdAndLeftAtIsNull(UUID userId);
    Optional<ChatMember> findByChatRoomIdAndUserIdAndLeftAtIsNull(UUID chatRoomId, UUID userId);
    boolean existsByChatRoomIdAndUserIdAndLeftAtIsNull(UUID chatRoomId, UUID userId);
}
