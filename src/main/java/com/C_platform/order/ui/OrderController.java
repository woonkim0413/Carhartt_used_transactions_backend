package com.C_platform.order.ui;

import com.C_platform.Member_woonkim.application.useCase.OAuth2UseCase;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
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

    private final CreateOrderService createOrderService;
    private final OrderCompletionService getOrderDetailsQuery;
    private final OrderCompletionService orderCompletionService;
    private final OAuth2UseCase oauth2UseCase;  // ✅ 추가

    @PostMapping(value = "/order",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createOrder(
            @Valid @RequestBody CreateOrderRequest req,
            HttpSession session, // X-Env X-Dev-User-Id 헤더 제거
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

        Long buyerId = member.getMemberId();


        // ✅ 1️⃣ requestId가 비어 있으면 UUID 생성
        String effectiveRequestId = (requestId != null && !requestId.isBlank())
                ? requestId
                : UUID.randomUUID().toString();

        // ✅ 2️⃣ 멱등성 키로 주문 생성 호출
        Long orderId = createOrderService.create(req.toOrderCommand(buyerId), effectiveRequestId);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-Request-Id",  effectiveRequestId);
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
        var body = ApiResponse.<CreateOrderResponse>fail(null, meta);
        return ResponseEntity.status(401).body(body);
    }

    private static String effectiveReqId(String reqId) {
        return (reqId != null && !reqId.isBlank()) ? reqId : "req-" + UUID.randomUUID();
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
