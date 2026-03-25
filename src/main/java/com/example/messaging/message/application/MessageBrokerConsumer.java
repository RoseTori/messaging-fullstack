package com.example.messaging.message.application;

import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageBrokerConsumer {

    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "messaging.message.queue")
    public void onMessageCreated(MessageCreatedEvent event) {
        messagingTemplate.convertAndSend("/topic/chat." + event.chatId(), event.message());
        for (UUID recipientId : event.recipientIds()) {
            messagingTemplate.convertAndSendToUser(recipientId.toString(), "/queue/messages", event.message());
        }
    }

    @RabbitListener(queues = "messaging.receipt.queue")
    public void onReceiptUpdated(ReceiptUpdatedEvent event) {
        messagingTemplate.convertAndSend("/topic/chat." + event.chatId() + ".receipts", event.status());
    }
}
