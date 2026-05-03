package com.bigbites.notification_service.service;

import com.bigbites.notification_service.entity.Notification;
import com.bigbites.notification_service.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getAll() {
        return notificationRepository.findAll();
    }

    public List<Notification> getByUser(Integer userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Notification> getByType(String type) {
        return notificationRepository.findByType(type);
    }

    public List<Notification> getByStatus(String status) {
        return notificationRepository.findByStatus(status);
    }

    public Notification markRead(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found: " + id));
        n.setStatus("READ");
        return notificationRepository.save(n);
    }

    public void delete(Long id) {
        notificationRepository.deleteById(id);
    }
}