package com.C_platform.order.ui;

import com.C_platform.order.application.dto.CreateOrderRequest;
import com.C_platform.order.application.dto.CreateOrderResponse;
import com.C_platform.order.application.dto.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Tag(name = "Order", description = "주문 API")
@RestController
@RequestMapping("/v1/order")
@RequiredArgsConstructor
public class OrderController {

    //주문 생성 api
    @PostMapping("/v1/order")
    public ApiResponse<CreateOrderResponse> createOrder(
            @RequestBody CreateOrderRequest req,
            HttpSession session,
            @RequestHeader("X-Request-Id") String requestId
    ){
        //아직 비즈니스 로직은 안 짰음
        //Long orderId = orderAppService.createOrder(user.getId(), req);
        //return ApiResponse.success(new CreateOrderResponse(orderId), requestId, UUID.randomUUID().toString());

        // Swagger 문서 확인용 더미 값
        Long orderId = 202L;
        return ApiResponse.success(
                new CreateOrderResponse(orderId),
                (requestId != null ? requestId : "req-auto"),
                UUID.randomUUID().toString()
        );
    }

}
