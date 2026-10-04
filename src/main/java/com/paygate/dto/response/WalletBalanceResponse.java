package com.paygate.dto.response;

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
public class WalletBalanceResponse {

    private Long walletId;
    private String userId;
    private BigDecimal balance;
    private String currency;
    private Long version;
    private LocalDateTime updatedAt;
}
