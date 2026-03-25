package com.example.messaging.realtime.api;

import com.example.messaging.config.WebSocketPrincipal;
import com.example.messaging.presence.application.PresenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
@RequiredArgsConstructor
public class WebSocketLifecycleListener {

    private final PresenceService presenceService;

    @EventListener
    public void onConnect(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof WebSocketPrincipal principal) {
            presenceService.registerSession(principal.userId(), accessor.getSessionId());
        }
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof WebSocketPrincipal principal) {
            presenceService.unregisterSession(principal.userId(), accessor.getSessionId());
        }
    }
}
