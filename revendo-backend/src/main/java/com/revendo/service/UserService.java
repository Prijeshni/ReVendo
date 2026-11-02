package com.revendo.service;

import com.revendo.dto.request.ChangePasswordRequest;
import com.revendo.dto.request.UpdateProfileRequest;
import com.revendo.dto.response.NotificationResponse;
import com.revendo.dto.response.TransactionResponse;
import com.revendo.dto.response.UserProfileResponse;
import com.revendo.exception.ResourceNotFoundException;
import com.revendo.exception.ValidationException;
import com.revendo.model.Notification;
import com.revendo.model.Transaction;
import com.revendo.model.User;
import com.revendo.repository.NotificationRepository;
import com.revendo.repository.TransactionRepository;
import com.revendo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Business logic for user profile and account management.
 *
 * Task 10.1 methods:
 *   getProfile         — return the authenticated user's profile
 *   updateProfile      — update first/last name and phone number
 *   changePassword     — verify current password, validate and hash the new one
 *   getPurchaseHistory — all PURCHASE transactions for the user
 *   getRentalHistory   — all RENTAL transactions for the user
 *   getNotifications   — all notifications for the user (most recent first)
 *
 * Requirements: 11.3, 11.4, 11.7, 11.8
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository         userRepository;
    private final TransactionRepository  transactionRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder        passwordEncoder;

    // ----------------------------------------------------------------
    // getProfile  (Requirement 11.7)
    // ----------------------------------------------------------------

    /**
     * Returns the profile for the authenticated user.
     *
     * @param userId the authenticated user's ID (from JWT)
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = loadUser(userId);
        return toProfileResponse(user);
    }

    // ----------------------------------------------------------------
    // updateProfile  (Requirement 11.7)
    // ----------------------------------------------------------------

    /**
     * Updates first name, last name, and phone number.
     *
     * @param userId the authenticated user's ID (from JWT)
     * @param dto    validated update payload
     */
    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest dto) {
        User user = loadUser(userId);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhoneNumber(dto.getPhoneNumber());
        return toProfileResponse(userRepository.save(user));
    }

    // ----------------------------------------------------------------
    // changePassword  (Requirement 11.8)
    // ----------------------------------------------------------------

    /**
     * Verifies the current password, then replaces it with a BCrypt-hashed
     * version of the new password.
     *
     * @param userId the authenticated user's ID (from JWT)
     * @param dto    validated change-password payload
     * @throws ValidationException if the current password is incorrect or the
     *                             new password does not meet the length requirement
     */
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest dto) {
        User user = loadUser(userId);

        // Verify current password
        if (!passwordEncoder.matches(dto.getCurrentPassword(), user.getPasswordHash())) {
            throw new ValidationException("Current password is incorrect.");
        }

        // Length guard (Bean Validation on the DTO also covers this, but belt-and-braces)
        if (dto.getNewPassword().length() < 8) {
            throw new ValidationException(
                    "New password must be at least 8 characters.");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getNewPassword()));
        userRepository.save(user);
    }

    // ----------------------------------------------------------------
    // getPurchaseHistory  (Requirement 11.3)
    // ----------------------------------------------------------------

    /**
     * All listings the user has purchased, most recent first.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getPurchaseHistory(Long userId) {
        return transactionRepository
                .findByBuyerIdAndTransactionType(userId, Transaction.TransactionType.PURCHASE)
                .stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // getRentalHistory  (Requirement 11.4)
    // ----------------------------------------------------------------

    /**
     * All listings the user has rented, most recent first.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponse> getRentalHistory(Long userId) {
        return transactionRepository
                .findByBuyerIdAndTransactionType(userId, Transaction.TransactionType.RENTAL)
                .stream()
                .map(this::toTransactionResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // getNotifications
    // ----------------------------------------------------------------

    /**
     * All notifications for the user, most recent first.
     */
    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotifications(Long userId) {
        return notificationRepository
                .findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::toNotificationResponse)
                .collect(Collectors.toList());
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    private User loadUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
    }

    private UserProfileResponse toProfileResponse(User u) {
        return UserProfileResponse.builder()
                .userId(u.getId())
                .firstName(u.getFirstName())
                .lastName(u.getLastName())
                .email(u.getEmail())
                .phoneNumber(u.getPhoneNumber())
                .role(u.getRole())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private TransactionResponse toTransactionResponse(Transaction t) {
        return TransactionResponse.builder()
                .transactionId(t.getId())
                .listingId(t.getListing().getId())
                .listingTitle(t.getListing().getTitle())
                .transactionType(t.getTransactionType())
                .pricePaid(t.getPricePaid())
                .transactionDate(t.getTransactionDate())
                .rentalStartDate(t.getRentalStartDate())
                .rentalEndDate(t.getRentalEndDate())
                .rentalDuration(t.getRentalDuration())
                .rentalPeriodUnit(t.getRentalPeriodUnit())
                .build();
    }

    private NotificationResponse toNotificationResponse(Notification n) {
        return NotificationResponse.builder()
                .notificationId(n.getId())
                .message(n.getMessage())
                .isRead(n.isRead())
                .createdAt(n.getCreatedAt())
                .build();
    }
}
