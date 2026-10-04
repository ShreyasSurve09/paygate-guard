package com.paygate.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paygate.dto.request.WebhookPaymentRequest;
import com.paygate.dto.response.DlqItemResponse;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.exception.DlqItemNotFoundException;
import com.paygate.model.entity.FailedWebhookQueue;
import com.paygate.model.enums.DlqStatus;
import com.paygate.repository.FailedWebhookQueueRepository;
import com.paygate.service.DlqService;
import com.paygate.service.IdempotencyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DlqServiceImpl implements DlqService {

    private final FailedWebhookQueueRepository dlqRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DlqServiceImpl(FailedWebhookQueueRepository dlqRepository, @Lazy IdempotencyService idempotencyService) {
        this.dlqRepository = dlqRepository;
        this.idempotencyService = idempotencyService;
    }

    @Override
    @Transactional
    public FailedWebhookQueue recordFailure(String idempotencyKey, String rawPayload, String errorMessage) {
        Optional<FailedWebhookQueue> existing = dlqRepository.findByIdempotencyKey(idempotencyKey);

        FailedWebhookQueue dlqItem;
        if (existing.isPresent()) {
            dlqItem = existing.get();
            dlqItem.setRetryCount(dlqItem.getRetryCount() + 1);
            dlqItem.setLastErrorMessage(errorMessage);
            // Exponential backoff: 2s, 4s, 8s, 16s...
            long delaySeconds = (long) Math.pow(2, Math.min(dlqItem.getRetryCount(), 6));
            dlqItem.setNextRetryAt(LocalDateTime.now().plusSeconds(delaySeconds));
            if (dlqItem.getRetryCount() >= dlqItem.getMaxRetries()) {
                dlqItem.setDlqStatus(DlqStatus.UNRESOLVED);
            }
        } else {
            dlqItem = FailedWebhookQueue.builder()
                    .idempotencyKey(idempotencyKey)
                    .rawPayload(rawPayload)
                    .retryCount(1)
                    .maxRetries(3)
                    .lastErrorMessage(errorMessage)
                    .dlqStatus(DlqStatus.UNRESOLVED)
                    .nextRetryAt(LocalDateTime.now().plusSeconds(2))
                    .build();
        }

        return dlqRepository.save(dlqItem);
    }

    @Override
    @Transactional(readOnly = true)
    public List<DlqItemResponse> getUnresolvedDlqItems() {
        return dlqRepository.findAll().stream()
                .filter(item -> item.getDlqStatus() == DlqStatus.UNRESOLVED)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public WebhookProcessResponse manuallyReprocess(Long dlqId) {
        FailedWebhookQueue dlqItem = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new DlqItemNotFoundException(dlqId));

        log.info("Admin initiated manual reprocessing for DLQ ID: {}", dlqId);

        try {
            WebhookPaymentRequest request = objectMapper.readValue(dlqItem.getRawPayload(), WebhookPaymentRequest.class);
            // Turn off simulated failure for manual resolution
            request.setSimulateFailure(false);

            WebhookProcessResponse response = idempotencyService.processPaymentWebhook(request);

            if (response.getStatus() == com.paygate.model.enums.PaymentStatus.SUCCESS ||
                response.getStatus() == com.paygate.model.enums.PaymentStatus.DUPLICATE_IGNORED) {
                dlqItem.setDlqStatus(DlqStatus.RESOLVED_MANUAL);
                dlqRepository.save(dlqItem);
                log.info("DLQ item {} successfully marked as RESOLVED_MANUAL", dlqId);
            }
            return response;
        } catch (Exception ex) {
            log.error("Manual reprocessing failed for DLQ ID {}: {}", dlqId, ex.getMessage());
            dlqItem.setLastErrorMessage(ex.getMessage());
            dlqRepository.save(dlqItem);
            throw new RuntimeException("Reprocessing failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional
    public void discardDlqItem(Long dlqId) {
        FailedWebhookQueue dlqItem = dlqRepository.findById(dlqId)
                .orElseThrow(() -> new DlqItemNotFoundException(dlqId));
        dlqItem.setDlqStatus(DlqStatus.DISCARDED);
        dlqRepository.save(dlqItem);
        log.warn("DLQ item {} marked as DISCARDED by admin", dlqId);
    }

    private DlqItemResponse mapToResponse(FailedWebhookQueue item) {
        return DlqItemResponse.builder()
                .id(item.getId())
                .idempotencyKey(item.getIdempotencyKey())
                .rawPayload(item.getRawPayload())
                .retryCount(item.getRetryCount())
                .lastErrorMessage(item.getLastErrorMessage())
                .dlqStatus(item.getDlqStatus())
                .createdAt(item.getCreatedAt())
                .lastRetriedAt(item.getUpdatedAt())
                .build();
    }
}
