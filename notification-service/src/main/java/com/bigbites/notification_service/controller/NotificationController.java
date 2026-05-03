package com.bigbites.notification_service.controller;

import com.bigbites.notification_service.entity.Notification;
import com.bigbites.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;  // FIX: was @Data @AllArgsConstructor
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<List<Notification>> getAll() {
        return ResponseEntity.ok(notificationService.getAll());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<Notification>> getByUser(@PathVariable Integer userId) {
        return ResponseEntity.ok(notificationService.getByUser(userId));
    }

    @GetMapping("/type/{type}")
    public ResponseEntity<List<Notification>> getByType(@PathVariable String type) {
        return ResponseEntity.ok(notificationService.getByType(type));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<Notification>> getByStatus(@PathVariable String status) {
        return ResponseEntity.ok(notificationService.getByStatus(status));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<Notification> markRead(@PathVariable Long id) {
        return ResponseEntity.ok(notificationService.markRead(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<String> delete(@PathVariable Long id) {
        notificationService.delete(id);
        return ResponseEntity.ok("Notification deleted");
    }
}