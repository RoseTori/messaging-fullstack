package com.example.messaging.realtime.api;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.chat.application.ChatService;
import com.example.messaging.config.WebSocketPrincipal;
import com.example.messaging.message.api.MessageReceiptRequest;
import com.example.messaging.message.api.MessageResponse;
import com.example.messaging.message.api.SendMessageRequest;
import com.example.messaging.message.application.MessageService;
import com.example.messaging.presence.application.PresenceService;
import com.example.messaging.presence.application.TypingService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final MessageService messageService;
    private final ChatService chatService;
    private final TypingService typingService;
    private final PresenceService presenceService;

    @MessageMapping("/chat.send")
    @SendToUser("/queue/ack")
    public MessageResponse send(@Payload SendMessageRequest request, WebSocketPrincipal principal) {
        chatService.assertMembership(request.chatId(), principal.userId());
        return messageService.send(request, new AuthenticatedUser(principal.userId(), principal.username(), ""));
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingRequest request, WebSocketPrincipal principal) {
        chatService.assertMembership(request.chatId(), principal.userId());
        AuthenticatedUser user = new AuthenticatedUser(principal.userId(), principal.username(), "");
        if (request.typing()) {
            typingService.typing(request.chatId(), user);
        } else {
            typingService.stopTyping(request.chatId(), user);
        }
    }

    @MessageMapping("/chat.receipt")
    public void receipt(@Payload MessageReceiptRequest request, WebSocketPrincipal principal) {
        messageService.updateReceipt(null, principal.userId(), request);
    }

    @MessageMapping("/presence.ping")
    public void ping(WebSocketPrincipal principal) {
        presenceService.heartbeat(principal.userId());
    }
}
