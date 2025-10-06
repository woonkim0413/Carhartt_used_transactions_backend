package com.C_platform.order.ui;

import com.C_platform.order.application.CreateOrderService;
import com.C_platform.order.application.OrderCompletionService;
import com.C_platform.order.ui.dto.ApiResponse;
import com.C_platform.order.ui.dto.CreateOrderRequest;
import com.C_platform.order.ui.dto.CreateOrderResponse;
import com.C_platform.order.ui.dto.OrderCompletionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;
import java.util.UUID;

@Tag(name = "Order", description = "주문 생성 API")
@RestController
@RequiredArgsConstructor
public class OrderController {

    private static final String SESSION_KEY_MEMBER_ID = "LOGIN_MEMBER_ID";

    private final CreateOrderService createOrderService;
    private final OrderCompletionService getOrderDetailsQuery;
    private final OrderCompletionService orderCompletionService;

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


    @Operation(summary = "결제 완료된 주문의 상세 상품 정보 조회")
    // 💡 GET /v1/orders/{orderId}/item 패턴 사용
    @GetMapping("/order/{orderId}/item")
    public ResponseEntity<OrderCompletionResponse> getCompletedOrderItem(
            @PathVariable Long orderId
            //@Authentication Long currentUserId // 현재 로그인한 사용자 (권한 검사용)
    ){
        // 🚨 currentUserId 대신 Mock 데이터 (예: 1L) 전달
        // 현재 Mock 사용으로, 권한 검사 로직은 서비스 계층에서 필요에 따라 추가해야 합니다.
        // Long currentUserId = 1L;

        try {
            // Service Layer 호출: orderId로 DB에서 Order 및 스냅샷 정보를 조회/검증 후 DTO 반환
            // 현재 OrderQueryService의 getOrderDetailsQuery(Long orderId) 메서드는 orderId만 받으므로 수정합니다.
            OrderCompletionResponse resp = orderCompletionService.getOrderDetailsQuery(orderId);

            return ResponseEntity.ok(resp);

        } catch (NoSuchElementException e) {
            // 주문 ID를 찾을 수 없을 때 (404 Not Found)
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalStateException e) {
            // 주문 상태가 PAID가 아닐 때 (400 Bad Request)
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            // 그 외 서버 오류 (500 Internal Server Error)
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
