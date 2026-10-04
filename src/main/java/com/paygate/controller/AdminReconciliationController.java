package com.paygate.controller;

import com.paygate.dto.response.DlqItemResponse;
import com.paygate.dto.response.WebhookProcessResponse;
import com.paygate.service.DlqService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/dlq")
@RequiredArgsConstructor
@Tag(name = "Admin DLQ & Reconciliation", description = "Operations to inspect, manually retry, and reconcile failed webhook transactions")
public class AdminReconciliationController {

    private final DlqService dlqService;

    @GetMapping("/unresolved")
    @Operation(summary = "List unresolved DLQ items", description = "Fetch all webhooks that failed downstream processing")
    public ResponseEntity<List<DlqItemResponse>> getUnresolvedDlqItems() {
        return ResponseEntity.ok(dlqService.getUnresolvedDlqItems());
    }

    @PostMapping("/{id}/reprocess")
    @Operation(summary = "Manually reprocess failed webhook", description = "Re-executes the webhook payment and updates status to RESOLVED_MANUAL")
    public ResponseEntity<WebhookProcessResponse> reprocessItem(@PathVariable Long id) {
        return ResponseEntity.ok(dlqService.manuallyReprocess(id));
    }

    @PostMapping("/{id}/discard")
    @Operation(summary = "Discard DLQ item", description = "Marks an invalid or unresolvable webhook entry as DISCARDED")
    public ResponseEntity<String> discardItem(@PathVariable Long id) {
        dlqService.discardDlqItem(id);
        return ResponseEntity.ok("DLQ item " + id + " has been marked as DISCARDED.");
    }
}
