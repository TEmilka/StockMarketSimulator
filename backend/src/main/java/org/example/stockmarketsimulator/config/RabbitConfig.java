package org.example.stockmarketsimulator.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String ASSET_QUEUE = "asset.price.update.queue";
    public static final String EXCHANGE = "asset.exchange";
    public static final String NOTIFICATION_QUEUE = "notification.queue";

    @Bean
    public Queue assetQueue() {
        return new Queue(ASSET_QUEUE, true);
    }

    @Bean
    public TopicExchange assetExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue assetQueue, TopicExchange assetExchange) {
        return BindingBuilder.bind(assetQueue).to(assetExchange).with("asset.price.updated");
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, TopicExchange assetExchange) {
        return BindingBuilder.bind(notificationQueue).to(assetExchange).with("notification.#");
    }
}

