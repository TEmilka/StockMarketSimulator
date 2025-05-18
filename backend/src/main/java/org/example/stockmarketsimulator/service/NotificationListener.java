package org.example.stockmarketsimulator.service;

import org.example.stockmarketsimulator.config.RabbitConfig;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.Channel;
import java.io.IOException;

@Service
public class NotificationListener {

    @RabbitListener(queues = RabbitConfig.QUEUE_NAME)
    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException, InterruptedException {
        try {
            System.out.println("Odebrano wiadomość: " + message);

            // Symulacja przetwarzania
            Thread.sleep(2000);

            // Potwierdzenie odbioru
            channel.basicAck(tag, false);
        } catch (Exception e) {
            // Odrzuć bez ponownego wstawiania do kolejki
            channel.basicNack(tag, false, false);
        }
    }
}
