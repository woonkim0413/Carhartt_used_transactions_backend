package com.C_platform.payment.ui;

import com.C_platform.Member_woonkim.application.useCase.OAuth2UseCase;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.payment.application.PaymentService;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
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
    private final OAuth2UseCase oauth2UseCase;  // ✅ 추가

    @Operation(summary = "결제 요청(ready)")
    @PostMapping(value = "/order/{orderId}/payment/ready")
    public ResponseEntity<ApiResponse<AttemptPaymentResponse>> attempt(
            @PathVariable Long orderId,
            @Valid @RequestBody AttemptPaymentRequest body,
            HttpSession session,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        // ✅ 세션에서 user 가져오기
        OAuth2UserInfoDto userInfo = (OAuth2UserInfoDto) session.getAttribute("user");
        if (userInfo == null) {
            return unauthorized(requestId);
        }

        // ✅ DB에서 Member 조회
        Member member = oauth2UseCase.getMemberBySessionInfo(userInfo);
        if (member == null) {
            return unauthorized(requestId);
        }

        Long currentUserId = member.getMemberId();

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
    @PostMapping(value = "/order/payment/approve")
    public ResponseEntity<ApiResponse<CompletePaymentResponse>> complete(
            @RequestParam("orderId") Long orderId,
            @Valid @RequestBody CompletePaymentRequest request,  // ✅ Body로 받기
            HttpSession session,  // ✅ 세션으로 변경
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        // ✅ 세션에서 user 가져오기
        OAuth2UserInfoDto userInfo = (OAuth2UserInfoDto) session.getAttribute("user");
        if (userInfo == null) {
            return unauthorizedComplete(requestId);
        }

        // ✅ DB에서 Member 조회
        Member member = oauth2UseCase.getMemberBySessionInfo(userInfo);
        if (member == null) {
            return unauthorizedComplete(requestId);
        }

        Long currentUserId = member.getMemberId();

        var data = paymentService.complete(orderId, request, currentUserId);
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        var response = ApiResponse.success(data, meta);

        return ResponseEntity.ok(response);
    }

    // ✅ 401 에러 응답 헬퍼 메서드 추가
    private ResponseEntity<ApiResponse<AttemptPaymentResponse>> unauthorized(String requestId) {
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        var body = ApiResponse.<AttemptPaymentResponse>fail(null, meta);
        return ResponseEntity.status(401).body(body);
    }

    private ResponseEntity<ApiResponse<CompletePaymentResponse>> unauthorizedComplete(String requestId) {
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        var body = ApiResponse.<CompletePaymentResponse>fail(null, meta);
        return ResponseEntity.status(401).body(body);
    }

    private static String effectiveReqId(String reqId) {
        return (reqId != null && !reqId.isBlank()) ? reqId : "req-" + UUID.randomUUID();
    }
}



