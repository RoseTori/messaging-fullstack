package com.example.messaging.message.application;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.chat.application.ChatService;
import com.example.messaging.common.application.NotFoundException;
import com.example.messaging.common.application.RateLimitService;
import com.example.messaging.common.domain.UuidV7;
import com.example.messaging.message.api.MessageReceiptRequest;
import com.example.messaging.message.api.MessageResponse;
import com.example.messaging.message.api.MessageStatusResponse;
import com.example.messaging.message.api.SendMessageRequest;
import com.example.messaging.message.domain.Message;
import com.example.messaging.message.domain.MessageDeliveryStatus;
import com.example.messaging.message.domain.MessageStatus;
import com.example.messaging.message.infrastructure.MessageRepository;
import com.example.messaging.message.infrastructure.MessageStatusRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final MessageStatusRepository messageStatusRepository;
    private final ChatService chatService;
    private final BrokerPublisher brokerPublisher;
    private final RateLimitService rateLimitService;

    @Value("${app.rate-limit.message-per-minute}")
    private int messagePerMinute;

    @Transactional
    public MessageResponse send(SendMessageRequest request, AuthenticatedUser sender) {
        rateLimitService.consume("rate-limit:message:" + sender.userId(), messagePerMinute, Duration.ofMinutes(1));
        chatService.assertMembership(request.chatId(), sender.userId());
        if (request.clientMessageId() != null) {
            var existing = messageRepository.findByChatRoomIdAndSenderIdAndClientMessageId(request.chatId(), sender.userId(), request.clientMessageId());
            if (existing.isPresent()) {
                return MessageResponse.from(existing.get());
            }
        }
        Message message = new Message();
        message.setId(UuidV7.randomUuid());
        message.setChatRoomId(request.chatId());
        message.setSenderId(sender.userId());
        message.setType(request.type());
        message.setCipherText(request.cipherText());
        message.setEncryptedKey(request.encryptedKey());
        message.setNonce(request.nonce());
        message.setAlgorithm(request.algorithm());
        message.setMetadata(request.metadata());
        message.setClientMessageId(request.clientMessageId());
        messageRepository.save(message);

        List<UUID> recipients = new ArrayList<>();
        for (UUID memberId : chatService.getActiveMemberIds(request.chatId())) {
            MessageStatus status = new MessageStatus();
            status.setId(UuidV7.randomUuid());
            status.setMessageId(message.getId());
            status.setUserId(memberId);
            status.setStatus(memberId.equals(sender.userId()) ? MessageDeliveryStatus.READ : MessageDeliveryStatus.SENT);
            messageStatusRepository.save(status);
            if (!memberId.equals(sender.userId())) {
                recipients.add(memberId);
            }
        }

        MessageResponse response = MessageResponse.from(message);
        brokerPublisher.publishMessage(new MessageCreatedEvent(request.chatId(), response, recipients));
        return response;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> history(UUID chatId, UUID userId, LocalDateTime before, int size) {
        chatService.assertMembership(chatId, userId);
        PageRequest pageRequest = PageRequest.of(0, Math.min(size, 100));
        List<Message> messages = before == null
                ? messageRepository.findByChatRoomIdOrderByCreatedAtDesc(chatId, pageRequest)
                : messageRepository.findByChatRoomIdAndCreatedAtBeforeOrderByCreatedAtDesc(chatId, before, pageRequest);
        return messages.stream().map(MessageResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public UUID getChatIdForMessage(UUID messageId) {
        return messageRepository.findById(messageId)
                .map(Message::getChatRoomId)
                .orElseThrow(() -> new NotFoundException("Message not found"));
    }

    @Transactional
    public MessageStatusResponse updateReceipt(UUID chatId, UUID userId, MessageReceiptRequest request) {
        if (chatId == null) {
            chatId = getChatIdForMessage(request.messageId());
        }
        chatService.assertMembership(chatId, userId);
        MessageStatus status = messageStatusRepository.findByMessageIdAndUserId(request.messageId(), userId)
                .orElseThrow(() -> new NotFoundException("Message status not found"));
        if (request.status().ordinal() < status.getStatus().ordinal()) {
            return MessageStatusResponse.from(status);
        }
        status.setStatus(request.status());
        status.setStatusAt(LocalDateTime.now());
        MessageStatus saved = messageStatusRepository.save(status);
        MessageStatusResponse response = MessageStatusResponse.from(saved);
        brokerPublisher.publishReceipt(new ReceiptUpdatedEvent(chatId, request.messageId(), response));
        return response;
    }

    @Transactional
    public int markDeliveredForUser(UUID userId) {
        return messageStatusRepository.bulkUpdateStatusForUser(userId, MessageDeliveryStatus.SENT, MessageDeliveryStatus.DELIVERED);
    }
}
