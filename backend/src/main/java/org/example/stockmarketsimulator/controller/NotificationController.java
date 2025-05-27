package org.example.stockmarketsimulator.controller;

import org.example.stockmarketsimulator.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<String>> getNotifications() {
        return ResponseEntity.ok(notificationService.getNotifications());
    }

    @DeleteMapping
    public ResponseEntity<Void> clearNotifications() {
        notificationService.clearNotifications();
        return ResponseEntity.noContent().build();
    }
}
