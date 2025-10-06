package com.C_platform.payment.ui;

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

@Tag(name = "Payment", description = "PG 연동 결제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/payment")  // ← 경로 변경
public class PaymentController {

    private final PaymentService paymentService;

    @Operation(summary = "결제 요청(ready)")
    @PostMapping(value = "/ready")  // → /v1/payment/ready
    public ResponseEntity<AttemptPaymentResponse> attempt(
            @Valid @RequestBody AttemptPaymentRequest body,
            @RequestHeader("X-Dev-User-Id") Long currentUserId
    ) {
        var resp = paymentService.ready(body, currentUserId);
        return ResponseEntity.ok(resp);
    }

    @Operation(summary = "결제 승인 완료")
    // 🚨 요청 URL 패턴과 정확히 일치하도록 경로를 수정했습니다.
    @PostMapping(value = "/order/{orderId}/payment/{pgToken}/approve")
    public ResponseEntity<CompletePaymentResponse> complete(
            // 🚨 @RequestBody 대신 URL 경로 변수에서 필수 값을 추출합니다.
            @PathVariable Long orderId,
            @PathVariable String pgToken,
            @RequestHeader("X-Dev-User-Id") Long currentUserId // 또는 @Authentication
    ) {
        // 🚨 URL에서 추출한 값으로 서비스 레이어에 필요한 DTO를 생성합니다.
        // CompletePaymentRequest는 provider, orderId, pgToken만 있다고 가정합니다.
        CompletePaymentRequest req = new CompletePaymentRequest(
                "KAKAOPAY",         // provider (PG사 코드)
                orderId,            // partner_order_id
                pgToken             // pg_token
        );

        var resp = paymentService.complete(req, currentUserId);
        return ResponseEntity.ok(resp);
    }
}



