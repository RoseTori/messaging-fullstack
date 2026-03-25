package com.example.messaging.message.application;

import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BrokerPublisher {

    private final RabbitTemplate rabbitTemplate;

    @Value("${app.broker.exchange}")
    private String exchange;

    @Value("${app.broker.message-routing-key}")
    private String messageRoutingKey;

    @Value("${app.broker.receipt-routing-key}")
    private String receiptRoutingKey;

    public void publishMessage(Object event) {
        rabbitTemplate.convertAndSend(exchange, messageRoutingKey, event);
    }

    public void publishReceipt(Object event) {
        rabbitTemplate.convertAndSend(exchange, receiptRoutingKey, event);
    }
}
