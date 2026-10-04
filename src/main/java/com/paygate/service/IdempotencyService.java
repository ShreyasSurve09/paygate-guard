package com.paygate.service;

import com.paygate.dto.request.WebhookPaymentRequest;
import com.paygate.dto.response.WebhookProcessResponse;

public interface IdempotencyService {

    WebhookProcessResponse processPaymentWebhook(WebhookPaymentRequest request);
}
