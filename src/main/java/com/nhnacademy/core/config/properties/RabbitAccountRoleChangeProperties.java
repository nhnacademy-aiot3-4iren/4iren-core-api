package com.nhnacademy.core.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "rabbitmq.account.role-change")
public class RabbitAccountRoleChangeProperties {
    private String exchange;
    private String routingKey;
    private String queue;
    private String deadLetterExchange;
    private String deadLetterRoutingKey;
    private String deadLetterQueue;
}
