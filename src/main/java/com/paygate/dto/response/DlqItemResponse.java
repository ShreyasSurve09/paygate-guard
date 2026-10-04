package com.paygate.dto.response;

import com.paygate.model.enums.DlqStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqItemResponse {

    private Long id;
    private String idempotencyKey;
    private String rawPayload;
    private int retryCount;
    private String lastErrorMessage;
    private DlqStatus dlqStatus;
    private LocalDateTime createdAt;
    private LocalDateTime lastRetriedAt;
}
