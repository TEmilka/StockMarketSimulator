package org.example.stockmarketsimulator.controller;

import org.example.stockmarketsimulator.service.NotificationSender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notify")
public class NotificationController {

    @Autowired
    private NotificationSender sender;

    @PostMapping
    public ResponseEntity<String> sendNotification(@RequestBody String msg) {
        sender.sendNotification(msg);
        return ResponseEntity.ok("Powiadomienie wysłane");
    }
}
