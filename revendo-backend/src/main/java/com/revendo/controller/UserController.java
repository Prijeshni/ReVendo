package com.revendo.controller;

import com.revendo.dto.request.ChangePasswordRequest;
import com.revendo.dto.request.UpdateProfileRequest;
import com.revendo.dto.response.NotificationResponse;
import com.revendo.dto.response.TransactionResponse;
import com.revendo.dto.response.UserProfileResponse;
import com.revendo.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST controller for authenticated user profile endpoints.
 *
 * All endpoints derive the user ID from the JWT — the frontend never supplies it.
 *
 * GET  /api/users/me                — view profile
 * PUT  /api/users/me                — update profile (name, phone)
 * PUT  /api/users/me/password       — change password
 * GET  /api/users/me/purchases      — purchase history
 * GET  /api/users/me/rentals        — rental history
 * GET  /api/users/me/notifications  — notifications list
 *
 * Requirements: 11.3, 11.4, 11.7, 11.8
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // GET /api/users/me
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(userService.getProfile(extractUserId(principal)));
    }

    // PUT /api/users/me
    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                userService.updateProfile(extractUserId(principal), request));
    }

    // PUT /api/users/me/password
    @PutMapping("/me/password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserDetails principal) {
        userService.changePassword(extractUserId(principal), request);
        return ResponseEntity.ok(Map.of("message", "Password updated successfully."));
    }

    // GET /api/users/me/purchases
    @GetMapping("/me/purchases")
    public ResponseEntity<List<TransactionResponse>> getPurchaseHistory(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                userService.getPurchaseHistory(extractUserId(principal)));
    }

    // GET /api/users/me/rentals
    @GetMapping("/me/rentals")
    public ResponseEntity<List<TransactionResponse>> getRentalHistory(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                userService.getRentalHistory(extractUserId(principal)));
    }

    // GET /api/users/me/notifications
    @GetMapping("/me/notifications")
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal UserDetails principal) {
        return ResponseEntity.ok(
                userService.getNotifications(extractUserId(principal)));
    }

    private Long extractUserId(UserDetails principal) {
        return Long.parseLong(principal.getUsername());
    }
}
