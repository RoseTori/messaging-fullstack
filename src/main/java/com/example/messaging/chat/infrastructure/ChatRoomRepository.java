package com.example.messaging.chat.infrastructure;

import com.example.messaging.chat.domain.ChatRoom;
import com.example.messaging.chat.domain.ChatType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChatRoomRepository extends JpaRepository<ChatRoom, UUID> {

    @Query("""
            select cr from ChatRoom cr
            where cr.type = :type
              and exists (select 1 from ChatMember cm1 where cm1.chatRoomId = cr.id and cm1.userId = :userA and cm1.leftAt is null)
              and exists (select 1 from ChatMember cm2 where cm2.chatRoomId = cr.id and cm2.userId = :userB and cm2.leftAt is null)
              and 2 = (select count(cm3) from ChatMember cm3 where cm3.chatRoomId = cr.id and cm3.leftAt is null)
            """)
    Optional<ChatRoom> findDirectChatBetween(@Param("userA") UUID userA, @Param("userB") UUID userB, @Param("type") ChatType type);
}
