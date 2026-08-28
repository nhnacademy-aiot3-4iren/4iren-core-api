package com.nhnacademy.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nhnacademy.core.config.properties.RabbitAccountRoleChangeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RabbitMQConfig {

    private final RabbitAccountRoleChangeProperties accountRoleChangeProperties;

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public DirectExchange accountRoleChangeExchange() {
        return new DirectExchange(accountRoleChangeProperties.getExchange());
    }

    @Bean
    public Queue accountRoleChangeQueue() {
        return QueueBuilder.durable(accountRoleChangeProperties.getQueue())
                .withArgument("x-dead-letter-exchange", accountRoleChangeProperties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", accountRoleChangeProperties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Binding accountRoleChangeBinding(Queue accountRoleChangeQueue, DirectExchange accountRoleChangeExchange) {
        return BindingBuilder.bind(accountRoleChangeQueue)
                .to(accountRoleChangeExchange)
                .with(accountRoleChangeProperties.getRoutingKey());
    }

    @Bean
    public DirectExchange accountRoleChangeDeadLetterExchange() {
        return new DirectExchange(accountRoleChangeProperties.getDeadLetterExchange());
    }

    @Bean
    public Queue accountRoleChangeDeadLetterQueue() {
        return QueueBuilder.durable(accountRoleChangeProperties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding accountRoleChangeDeadLetterBinding(Queue accountRoleChangeDeadLetterQueue, DirectExchange accountRoleChangeDeadLetterExchange) {
        return BindingBuilder.bind(accountRoleChangeDeadLetterQueue)
                .to(accountRoleChangeDeadLetterExchange)
                .with(accountRoleChangeProperties.getDeadLetterRoutingKey());
    }
}
