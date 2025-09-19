package com.C_platform.order.ui.dto;

import com.C_platform.order.application.dto.CreateOrderCommand;
import com.C_platform.payment.application.dto.PaymentReadyCommand;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * FE에서 보내는 주문 생성 요청 바디.
 * - payment_method는 '결제 준비(ready)' 단계에서 사용할 값이지만,
 *   FE 스펙을 맞추기 위해 여기서 함께 받음.
 * - 컨트롤러에서 내부 UseCase로 넘길 때는 아래 변환 메서드를 사용:
 *   1) toOrderCommand(...)  : 주문 생성 용
 *   2) toPaymentReadyCommand(...): 결제 준비 용
 */
@Schema(name="CreateOrderRequest")
public record CreateOrderRequest(

        @NotNull @Positive
        @Schema(example = "1101")
        @JsonProperty("item_id") Long itemId,

        @NotNull @Positive
        @Schema(example = "101")
        @JsonProperty("address_id") Long addressId,

        @Schema(allowableValues = {"KAKAOPAY","NAVERPAY"}, example = "KAKAOPAY")
        @JsonProperty("payment_method") String paymentMethod,

        @Schema(example = "~")
        @JsonProperty("detail_message") String detailMessage
) {
    /** 주문 생성 유스케이스에 전달할 내부 커맨드로 변환 (paymentMethod는 제외) */
    public CreateOrderCommand toOrderCommand(Long buyerId) {
        return new CreateOrderCommand(buyerId, itemId, addressId, detailMessage);
    }

    /**
     * 결제 준비 유스케이스에 전달할 내부 커맨드로 변환
     * - orderId는 방금 생성된 주문 ID를 컨트롤러에서 받아 넣는다.
     */
    public PaymentReadyCommand toPaymentReadyCommand(Long orderId) {
        return new PaymentReadyCommand(orderId, paymentMethod);
    }
}


