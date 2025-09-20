package com.C_platform.order.ui;

import com.C_platform.order.application.CreateOrderService;
import com.C_platform.order.ui.dto.ApiResponse;
import com.C_platform.order.ui.dto.CreateOrderRequest;
import com.C_platform.order.ui.dto.CreateOrderResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Tag(name = "Order", description = "주문 생성 API")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private static final String SESSION_KEY_MEMBER_ID = "LOGIN_MEMBER_ID";

    private final CreateOrderService createOrderService;

    // FE 스펙: POST /api/order
    @PostMapping(value = "/api/order",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            HttpSession session,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) Long devUserId,  // 임시 우회용
            @RequestHeader(value = "X-Env", required = false) String env               // "dev" 일 때만 허용
    ) {
        // 1) buyerId 결정: 세션 → (없으면) 개발환경에서만 헤더로 대체
        Long buyerId = (Long) session.getAttribute(SESSION_KEY_MEMBER_ID);
        if (buyerId == null) {
            if ("dev".equalsIgnoreCase(env) && devUserId != null) {
                buyerId = devUserId; // 임시 우회
            } else {
                return unauthorized(requestId);
            }
        }

        // 2) 서비스 호출 (CreateOrderRequest -> CreateOrderCommand 변환 사용)
        Long orderId = createOrderService.create(req.toOrderCommand(buyerId));

        // 3) 응답 헤더
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", effectiveReqId(requestId));
        headers.add("X-Server-Timezone", "UTC");
        headers.add("X-Server-Time-Format", "ISO-8601");

        // 4) 응답 바디
        var body = ApiResponse.success(
                CreateOrderResponse.of(orderId),
                effectiveReqId(requestId),
                genTraceId()
        );

        return ResponseEntity.ok().headers(headers).body(body);
    }

    private ResponseEntity<ApiResponse<CreateOrderResponse>> unauthorized(String requestId) {
        var body = ApiResponse.<CreateOrderResponse>fail(effectiveReqId(requestId), genTraceId());
        return ResponseEntity.status(401).body(body);
    }



    private static String effectiveReqId(String reqId) {
        return (reqId != null && !reqId.isBlank()) ? reqId : "req-" + UUID.randomUUID();
    }

    private static String genTraceId() {
        return "trc-" + UUID.randomUUID();
    }
}
