package com.example.messaging.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final String allowedOrigins;
    private final WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor;
    private final String relayHost;
    private final Integer relayPort;
    private final String relayUsername;
    private final String relayPassword;

    public WebSocketConfig(@Value("${app.websocket.allowed-origins}") String allowedOrigins,
                           WebSocketAuthChannelInterceptor webSocketAuthChannelInterceptor,
                           @Value("${app.broker.relay-host}") String relayHost,
                           @Value("${app.broker.relay-port}") Integer relayPort,
                           @Value("${app.broker.relay-username}") String relayUsername,
                           @Value("${app.broker.relay-password}") String relayPassword) {
        this.allowedOrigins = allowedOrigins;
        this.relayHost = relayHost;
        this.relayPort = relayPort;
        this.relayUsername = relayUsername;
        this.relayPassword = relayPassword;
        this.webSocketAuthChannelInterceptor = webSocketAuthChannelInterceptor;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").setAllowedOriginPatterns(allowedOrigins);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthChannelInterceptor);
    }


    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost(relayHost)
                .setRelayPort(relayPort)
                .setClientLogin(relayUsername)
                .setClientPasscode(relayPassword)
                .setSystemLogin(relayUsername)
                .setSystemPasscode(relayPassword);
    }
}
