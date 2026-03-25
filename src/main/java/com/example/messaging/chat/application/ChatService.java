package com.example.messaging.chat.application;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.chat.api.ChatResponse;
import com.example.messaging.chat.api.CreateChatRequest;
import com.example.messaging.chat.domain.ChatMember;
import com.example.messaging.chat.domain.ChatRole;
import com.example.messaging.chat.domain.ChatRoom;
import com.example.messaging.chat.domain.ChatType;
import com.example.messaging.chat.infrastructure.ChatMemberRepository;
import com.example.messaging.chat.infrastructure.ChatRoomRepository;
import com.example.messaging.common.application.ForbiddenOperationException;
import com.example.messaging.common.application.NotFoundException;
import com.example.messaging.common.domain.UuidV7;
import com.example.messaging.moderation.application.BlockService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ChatMemberRepository chatMemberRepository;
    private final BlockService blockService;

    @Transactional
    public ChatResponse createChat(CreateChatRequest request, AuthenticatedUser authenticatedUser) {
        Set<UUID> memberIds = new LinkedHashSet<>(request.memberIds());
        memberIds.remove(authenticatedUser.userId());
        if (request.type() == ChatType.DIRECT && memberIds.size() != 1) {
            throw new IllegalArgumentException("Direct chats require exactly one other member");
        }
        if (request.type() == ChatType.DIRECT) {
            UUID otherUserId = memberIds.iterator().next();
            blockService.assertCanMessage(authenticatedUser.userId(), otherUserId);
            var existingDirect = chatRoomRepository.findDirectChatBetween(authenticatedUser.userId(), otherUserId, ChatType.DIRECT);
            if (existingDirect.isPresent()) {
                return toResponse(existingDirect.get().getId());
            }
        }
        ChatRoom room = new ChatRoom();
        room.setId(UuidV7.randomUuid());
        room.setType(request.type());
        room.setTitle(request.type() == ChatType.GROUP ? request.title() : null);
        room.setCreatedBy(authenticatedUser.userId());
        chatRoomRepository.save(room);

        List<UUID> allMembers = new ArrayList<>();
        allMembers.add(authenticatedUser.userId());
        allMembers.addAll(memberIds);
        for (UUID userId : allMembers) {
            ChatMember member = new ChatMember();
            member.setId(UuidV7.randomUuid());
            member.setChatRoomId(room.getId());
            member.setUserId(userId);
            member.setRole(userId.equals(authenticatedUser.userId()) ? ChatRole.ADMIN : ChatRole.MEMBER);
            chatMemberRepository.save(member);
        }
        return new ChatResponse(room.getId(), room.getType(), room.getTitle(), allMembers, room.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<ChatResponse> getMyChats(UUID userId) {
        return chatMemberRepository.findByUserIdAndLeftAtIsNull(userId).stream()
                .map(member -> toResponse(member.getChatRoomId()))
                .sorted(Comparator.comparing(ChatResponse::createdAt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public ChatResponse getChat(UUID chatId, UUID userId) {
        assertMembership(chatId, userId);
        return toResponse(chatId);
    }

    public void assertMembership(UUID chatId, UUID userId) {
        if (!chatMemberRepository.existsByChatRoomIdAndUserIdAndLeftAtIsNull(chatId, userId)) {
            throw new ForbiddenOperationException("User is not a member of this chat");
        }
    }

    @Transactional(readOnly = true)
    public List<UUID> getActiveMemberIds(UUID chatId) {
        return chatMemberRepository.findByChatRoomIdAndLeftAtIsNull(chatId).stream().map(ChatMember::getUserId).toList();
    }

    private ChatResponse toResponse(UUID chatId) {
        ChatRoom room = chatRoomRepository.findById(chatId).orElseThrow(() -> new NotFoundException("Chat not found"));
        return new ChatResponse(room.getId(), room.getType(), room.getTitle(), getActiveMemberIds(chatId), room.getCreatedAt());
    }
}
