package com.paygate.service;

import com.paygate.dto.response.DlqItemResponse;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.model.entity.FailedWebhookQueue;

import java.util.List;

public interface DlqService {

    FailedWebhookQueue recordFailure(String idempotencyKey, String rawPayload, String errorMessage);

    List<DlqItemResponse> getUnresolvedDlqItems();

    WebhookProcessResponse manuallyReprocess(Long dlqId);

    void discardDlqItem(Long dlqId);
}
