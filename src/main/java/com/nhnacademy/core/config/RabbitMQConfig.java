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
    private final com.nhnacademy.core.config.properties.RabbitAccountAdminCreateProperties adminCreateProperties;

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



    @Bean
    public DirectExchange adminCreateExchange() {
        return new DirectExchange(adminCreateProperties.getExchange());
    }

    @Bean
    public Queue adminCreateQueue() {
        return QueueBuilder.durable(adminCreateProperties.getQueue())
                .withArgument("x-dead-letter-exchange", adminCreateProperties.getDeadLetterExchange())
                .withArgument("x-dead-letter-routing-key", adminCreateProperties.getDeadLetterRoutingKey())
                .build();
    }

    @Bean
    public Binding adminCreateBinding(Queue adminCreateQueue, DirectExchange adminCreateExchange) {
        return BindingBuilder.bind(adminCreateQueue)
                .to(adminCreateExchange)
                .with(adminCreateProperties.getRoutingKey());
    }

    @Bean
    public DirectExchange adminCreateDeadLetterExchange() {
        return new DirectExchange(adminCreateProperties.getDeadLetterExchange());
    }

    @Bean
    public Queue adminCreateDeadLetterQueue() {
        return QueueBuilder.durable(adminCreateProperties.getDeadLetterQueue()).build();
    }

    @Bean
    public Binding adminCreateDeadLetterBinding(Queue adminCreateDeadLetterQueue, DirectExchange adminCreateDeadLetterExchange) {
        return BindingBuilder.bind(adminCreateDeadLetterQueue)
                .to(adminCreateDeadLetterExchange)
                .with(adminCreateProperties.getDeadLetterRoutingKey());
    }
}
