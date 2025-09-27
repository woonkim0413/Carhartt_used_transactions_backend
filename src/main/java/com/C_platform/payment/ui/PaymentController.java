package com.C_platform.payment.ui;

import com.C_platform.payment.application.PaymentStubService;
import com.C_platform.payment.ui.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Payment (Stub)", description = "PG 연동 없이 Swagger에서 확인하는 깡통 결제 API")
@RestController
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentStubService paymentStubService;

    // 결제 요청 (스펙: POST /v1/order/{order_id}/payment/ready)
    @Operation(summary = "결제 요청(ready) — Stub")
    @PostMapping(
            value = "/v1/order/{order_id}/payment/ready",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<AttemptPaymentResponse> ready(
            @PathVariable("order_id") Long orderId,
            @Valid @RequestBody AttemptPaymentRequest body,
            HttpServletRequest http
    ) {
        System.out.println("### HIT /ready orderId=" + orderId); // ← 이거
        // path vs body 일치 검증 (팀 규칙)
        if (!orderId.equals(body.orderId())) {
            return ResponseEntity.badRequest().build();
        }
        var resp = paymentStubService.ready(body);
        return ResponseEntity.ok(resp);
    }

    // 결제 완료 (스펙 유지: 바디에 merchant_uid 등으로 처리)
    @Operation(summary = "결제 완료(approve/fail/cancel) — Stub")
    @PostMapping(
            value = "/v1/order/{order_id}/payment/{tid}/approve",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<CompletePaymentResponse> complete(
            @PathVariable("order_id") Long orderId,
            @PathVariable("tid") String tid,
            @Valid @RequestBody CompletePaymentRequest body
    ) {
        System.out.println("### HIT /approve orderId=" + orderId + " tid=" + tid);

        var resp = paymentStubService.complete(body);
        return ResponseEntity.ok(resp);
    }

}

