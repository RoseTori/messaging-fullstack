package com.example.messaging.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(converter);
        return rabbitTemplate;
    }

    @Bean
    public DirectExchange messagingExchange(@Value("${app.broker.exchange}") String exchangeName) {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue messageQueue() {
        return new Queue("messaging.message.queue", true);
    }

    @Bean
    public Queue receiptQueue() {
        return new Queue("messaging.receipt.queue", true);
    }

    @Bean
    public Binding messageBinding(DirectExchange messagingExchange,
                                  @Value("${app.broker.message-routing-key}") String routingKey) {
        return BindingBuilder.bind(messageQueue()).to(messagingExchange).with(routingKey);
    }

    @Bean
    public Binding receiptBinding(DirectExchange messagingExchange,
                                  @Value("${app.broker.receipt-routing-key}") String routingKey) {
        return BindingBuilder.bind(receiptQueue()).to(messagingExchange).with(routingKey);
    }
}
