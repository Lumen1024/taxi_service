package com.lumen1024.notification_service.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue tripEventQueue() {
        return new Queue("trip.events", true);
    }

    @Bean
    public TopicExchange tripExchange() {
        return new TopicExchange("trip.exchange");
    }

    @Bean
    public Binding tripEventBinding(Queue tripEventQueue, TopicExchange tripExchange) {
        return BindingBuilder.bind(tripEventQueue).to(tripExchange).with("trip.event");
    }
}
