package com.paygate.controller;

import com.paygate.dto.request.WebhookPaymentRequest;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.service.IdempotencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payment Webhooks", description = "Endpoints for receiving and idempotently processing gateway payment webhooks")
public class WebhookController {

    private final IdempotencyService idempotencyService;

    @PostMapping("/payment")
    @Operation(summary = "Receive and process payment gateway webhook", 
               description = "Guarantees idempotency via unique idempotencyKey. Prevents double-credits upon webhook retries.")
    public ResponseEntity<WebhookProcessResponse> handlePaymentWebhook(@Valid @RequestBody WebhookPaymentRequest request) {
        log.info("Received payment webhook for key: {}", request.getIdempotencyKey());
        WebhookProcessResponse response = idempotencyService.processPaymentWebhook(request);
        return ResponseEntity.ok(response);
    }
}
