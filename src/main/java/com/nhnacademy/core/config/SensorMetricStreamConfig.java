package com.nhnacademy.core.config;

import com.nhnacademy.core.property.SensorMetricStreamProperties;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SensorMetricStreamProperties.class)
public class SensorMetricStreamConfig {

    @Bean("processingSensorExchange")
    public TopicExchange processingSensorExchange(
            SensorMetricStreamProperties properties
    ) {
        return new TopicExchange(properties.source().exchange(), true, false);
    }

    @Bean("sensorMetricStreamQueue")
    public Queue sensorMetricStreamQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public Binding sensorMetricStreamBinding(
            @Qualifier("sensorMetricStreamQueue") Queue queue,
            @Qualifier("processingSensorExchange") TopicExchange exchange,
            SensorMetricStreamProperties properties
    ) {
        return BindingBuilder.bind(queue)
                .to(exchange)
                .with(properties.source().bindingKey());
    }

    @Bean("sensorMetricStreamTaskScheduler")
    public ThreadPoolTaskScheduler sensorMetricStreamTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("sensor-metric-stream-");
        scheduler.setRemoveOnCancelPolicy(true);
        scheduler.setWaitForTasksToCompleteOnShutdown(false);
        return scheduler;
    }

    @Bean("sensorMetricSseSendExecutor")
    public AsyncTaskExecutor sensorMetricSseSendExecutor() {
        return new VirtualThreadTaskExecutor("sensor-metric-sse-send-");
    }
}
