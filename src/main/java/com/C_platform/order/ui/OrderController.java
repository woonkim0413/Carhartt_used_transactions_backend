package com.C_platform.order.ui;

import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.order.application.CreateOrderService;
import com.C_platform.order.application.OrderCompletionService;
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

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.UUID;

@Tag(name = "Order", description = "주문 생성 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1")
public class OrderController {

    private static final String SESSION_KEY_MEMBER_ID = "LOGIN_MEMBER_ID";

    private final CreateOrderService createOrderService;
    private final OrderCompletionService getOrderDetailsQuery;
    private final OrderCompletionService orderCompletionService;

    @PostMapping(value = "/order",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            HttpSession session,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId,
            @RequestHeader(value = "X-Dev-User-Id", required = false) Long devUserId,
            @RequestHeader(value = "X-Env", required = false) String env
    ) {
        Long buyerId = (Long) session.getAttribute(SESSION_KEY_MEMBER_ID);
        if (buyerId == null) {
            if ("dev".equalsIgnoreCase(env) && devUserId != null) {
                buyerId = devUserId;
            } else {
                return unauthorized(requestId);
            }
        }

        Long orderId = createOrderService.create(req.toOrderCommand(buyerId));

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id", effectiveReqId(requestId));
        headers.add("X-Server-Timezone", "UTC");
        headers.add("X-Server-Time-Format", "ISO-8601");

        var data = CreateOrderResponse.of(orderId);
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        var body = ApiResponse.success(data, meta);

        return ResponseEntity.ok().headers(headers).body(body);
    }

    private ResponseEntity<ApiResponse<CreateOrderResponse>> unauthorized(String requestId) {
        var meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .xRequestId(effectiveReqId(requestId))
                .build();
        // TODO: ErrorBody 구조에 맞게 수정 필요
        var body = ApiResponse.<CreateOrderResponse>fail(null, meta);
        return ResponseEntity.status(401).body(body);
    }

    private static String effectiveReqId(String reqId) {
        return (reqId != null && !reqId.isBlank()) ? reqId : "req-" + UUID.randomUUID();
    }

    private static String genTraceId() {
        return "trc-" + UUID.randomUUID();
    }

    @Operation(summary = "결제 완료된 주문의 상세 상품 정보 조회")
    @GetMapping("/order/{orderId}/item")
    public ResponseEntity<ApiResponse<OrderCompletionResponse>> getCompletedOrderItem(
            @PathVariable Long orderId,
            @RequestHeader(value = "X-Request-Id", required = false) String requestId
    ) {
        try {
            OrderCompletionResponse data = orderCompletionService.getOrderDetailsQuery(orderId);

            var meta = MetaData.builder()
                    .timestamp(LocalDateTime.now())
                    .xRequestId(effectiveReqId(requestId))
                    .build();
            var response = ApiResponse.success(data, meta);

            return ResponseEntity.ok(response);

        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();

        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
