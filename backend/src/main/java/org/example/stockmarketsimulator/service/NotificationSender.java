package org.example.stockmarketsimulator.service;

import org.example.stockmarketsimulator.config.RabbitConfig;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class NotificationSender {

    private final RabbitTemplate rabbitTemplate;

    @Autowired
    public NotificationSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendNotification(String message) {
        rabbitTemplate.convertAndSend(RabbitConfig.QUEUE_NAME, message);
        System.out.println("Wysłano wiadomość: " + message);
    }
}
