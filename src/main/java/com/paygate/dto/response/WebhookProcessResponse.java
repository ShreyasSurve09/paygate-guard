package com.paygate.dto.response;

import com.paygate.model.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookProcessResponse {

    private String idempotencyKey;
    private PaymentStatus status;
    private String message;
    private Long transactionId;
    private BigDecimal creditedAmount;
    private BigDecimal currentWalletBalance;
    private boolean duplicateIgnored;
    private LocalDateTime processedAt;
}
