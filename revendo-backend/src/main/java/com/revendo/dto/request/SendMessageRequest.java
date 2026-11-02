package com.revendo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Incoming payload for POST /api/messages/conversations/:id.
 * Requirements: 9.4
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SendMessageRequest {

    @NotBlank(message = "Message text is required.")
    @Size(max = 2000, message = "Message must not exceed 2000 characters.")
    private String text;
}
