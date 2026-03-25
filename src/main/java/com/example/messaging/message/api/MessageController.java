package com.example.messaging.message.api;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.message.application.MessageService;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats/{chatId}/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping
    public MessageResponse send(@PathVariable UUID chatId,
                                @Valid @RequestBody SendMessageRequest request,
                                @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        if (!chatId.equals(request.chatId())) {
            throw new IllegalArgumentException("Path chatId and body chatId must match");
        }
        return messageService.send(request, authenticatedUser);
    }

    @GetMapping
    public List<MessageResponse> history(@PathVariable UUID chatId,
                                         @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before,
                                         @RequestParam(defaultValue = "50") int size) {
        return messageService.history(chatId, authenticatedUser.userId(), before, size);
    }

    @PostMapping("/receipts")
    public MessageStatusResponse receipt(@PathVariable UUID chatId,
                                         @AuthenticationPrincipal AuthenticatedUser authenticatedUser,
                                         @Valid @RequestBody MessageReceiptRequest request) {
        return messageService.updateReceipt(chatId, authenticatedUser.userId(), request);
    }
}
