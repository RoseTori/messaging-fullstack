package com.example.messaging.moderation.api;

import com.example.messaging.auth.application.AuthenticatedUser;
import com.example.messaging.moderation.application.BlockService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/{userId}/block")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping
    public void block(@PathVariable UUID userId,
                      @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        blockService.block(authenticatedUser.userId(), userId);
    }

    @DeleteMapping
    public void unblock(@PathVariable UUID userId,
                        @AuthenticationPrincipal AuthenticatedUser authenticatedUser) {
        blockService.unblock(authenticatedUser.userId(), userId);
    }
}
