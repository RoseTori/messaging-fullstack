package com.example.messaging.chat.api;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.chat.application.ChatService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chats")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ChatResponse create(@Valid @RequestBody CreateChatRequest request,
                               @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return chatService.createChat(request, authenticatedUser);
    }

    @GetMapping
    public List<ChatResponse> mine(@AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return chatService.getMyChats(authenticatedUser.userId());
    }

    @GetMapping("/{chatId}")
    public ChatResponse get(@PathVariable UUID chatId,
                            @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        return chatService.getChat(chatId, authenticatedUser.userId());
    }
}
