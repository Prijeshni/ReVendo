package com.revendo.repository;

import com.revendo.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Notification} entities.
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * All notifications for a user, most recent first.
     */
    List<Notification> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * All unread notifications for a user.
     */
    List<Notification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(Long userId);

    /**
     * Count of unread notifications for a user.
     */
    long countByUserIdAndIsReadFalse(Long userId);
}
