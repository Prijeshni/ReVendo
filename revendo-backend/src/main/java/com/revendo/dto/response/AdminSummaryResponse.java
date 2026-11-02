package com.revendo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Outgoing payload for GET /api/admin/summary.
 * Requirements: 13.3
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminSummaryResponse {

    private long totalUsers;
    private long totalActiveListings;
    private long totalCompletedTransactions;
    /** Count of listings with status Suspended (pending admin review). */
    private long totalListingsPendingReview;
}
