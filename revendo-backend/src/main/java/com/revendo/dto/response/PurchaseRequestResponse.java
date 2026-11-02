package com.revendo.dto.response;

import com.revendo.model.PurchaseRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseRequestResponse {
    private Long                              requestId;
    private Long                             listingId;
    private String                           listingTitle;
    private String                           listingImageUrl;
    private Long                             buyerId;
    private String                           buyerName;
    private Long                             sellerId;
    private String                           sellerName;
    private BigDecimal                       price;
    private PurchaseRequest.RequestType       requestType;
    private Integer                          rentalDuration;
    private PurchaseRequest.RequestStatus    status;
    private LocalDateTime                    requestDate;
    private LocalDateTime                    responseDate;
}
