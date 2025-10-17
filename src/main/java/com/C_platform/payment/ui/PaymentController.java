package com.C_platform.payment.ui;

import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.payment.application.PaymentService;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Tag(name = "Payment", description = "PG 연동 결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청(ready)")
    @PostMapping(value = "/order/{orderId}/payment/ready")
    public ResponseEntity<ApiResponse<AttemptPaymentResponse>> attempt(
            @PathVariable Long orderId,
            @Valid @RequestBody AttemptPaymentRequest body,
            @RequestHeader("X-Dev-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        if (!orderId.equals(body.orderId())) {
            return ResponseEntity.badRequest().build();
        }

        var data = paymentService.ready(body, currentUserId);
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        var response = ApiResponse.success(data, meta);

        return ResponseEntity.ok(response);
    }

    @Operation(summary = "결제 승인 완료")
    @PostMapping(value = "/order/{orderId}/payment/{provider}/{pgToken}/approve")
    public ResponseEntity<ApiResponse<CompletePaymentResponse>> complete(
            @PathVariable Long orderId,
            @PathVariable String provider,
            @PathVariable String pgToken,
            @RequestHeader("X-Dev-User-Id") Long currentUserId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        CompletePaymentRequest req = new CompletePaymentRequest(
                provider,
                orderId,
                pgToken
        );

        var data = paymentService.complete(req, currentUserId);
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        var response = ApiResponse.success(data, meta);

        return ResponseEntity.ok(response);
    }

    private static String effectiveReqId(String reqId) {
        return (reqId != null && !reqId.isBlank()) ? reqId : "req-" + UUID.randomUUID();
    }

    private static String genTraceId() {
        return "trc-" + UUID.randomUUID();
    }
}



