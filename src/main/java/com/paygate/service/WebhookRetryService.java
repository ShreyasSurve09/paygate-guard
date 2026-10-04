package com.paygate.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.paygate.dto.request.WebhookPaymentRequest;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.model.entity.FailedWebhookQueue;
import com.paygate.model.enums.DlqStatus;
import com.paygate.model.enums.PaymentStatus;
import com.paygate.repository.FailedWebhookQueueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduled background worker that polls the DLQ for failed webhooks due for retry.
 * Implements exponential backoff to avoid hammering downstream banking endpoints.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookRetryService {

    private final FailedWebhookQueueRepository dlqRepository;
    private final IdempotencyService idempotencyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Runs every 15 seconds
    @Scheduled(fixedDelay = 15000)
    public void retryPendingWebhooks() {
        LocalDateTime now = LocalDateTime.now();
        List<FailedWebhookQueue> pendingRetries = dlqRepository.findByDlqStatusAndNextRetryAtBefore(DlqStatus.UNRESOLVED, now);

        if (pendingRetries.isEmpty()) {
            return;
        }

        log.info("Found {} failed webhooks eligible for exponential retry", pendingRetries.size());

        for (FailedWebhookQueue item : pendingRetries) {
            if (item.getRetryCount() >= item.getMaxRetries()) {
                log.warn("Item {} exceeded maximum retry count ({}). Leaving for manual DLQ review.", item.getId(), item.getMaxRetries());
                continue;
            }

            log.info("Executing scheduled retry attempt #{} for DLQ ID: {}", item.getRetryCount() + 1, item.getId());
            try {
                WebhookPaymentRequest request = objectMapper.readValue(item.getRawPayload(), WebhookPaymentRequest.class);
                request.setSimulateFailure(false); // attempt real clearance

                WebhookProcessResponse response = idempotencyService.processPaymentWebhook(request);
                if (response.getStatus() == PaymentStatus.SUCCESS || response.getStatus() == PaymentStatus.DUPLICATE_IGNORED) {
                    item.setDlqStatus(DlqStatus.RESOLVED_MANUAL);
                    dlqRepository.save(item);
                    log.info("Scheduled retry SUCCEEDED for DLQ ID: {}", item.getId());
                }
            } catch (Exception ex) {
                log.error("Scheduled retry FAILED for DLQ ID {}: {}", item.getId(), ex.getMessage());
                item.setRetryCount(item.getRetryCount() + 1);
                item.setLastErrorMessage(ex.getMessage());
                long nextDelay = (long) Math.pow(2, Math.min(item.getRetryCount(), 6));
                item.setNextRetryAt(LocalDateTime.now().plusSeconds(nextDelay));
                dlqRepository.save(item);
            }
        }
    }
}
