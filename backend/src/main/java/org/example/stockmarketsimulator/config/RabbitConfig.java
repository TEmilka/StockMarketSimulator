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

    @Bean
    public Queue assetQueue() {
        return new Queue(ASSET_QUEUE, true); // trwała kolejka
    }

    @Bean
    public TopicExchange assetExchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue assetQueue, TopicExchange assetExchange) {
        return BindingBuilder.bind(assetQueue).to(assetExchange).with("asset.price.updated");
    }
}

