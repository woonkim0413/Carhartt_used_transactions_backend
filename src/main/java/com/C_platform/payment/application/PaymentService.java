package com.C_platform.payment.application;

import com.C_platform.exception.PaymentException;
import com.C_platform.global.error.PaymentErrorCode;
import com.C_platform.order.application.port.ItemPricingReader;
import com.C_platform.order.domain.Order;
import com.C_platform.order.domain.OrderRepository;
import com.C_platform.item.domain.Item;
import com.C_platform.item.infrastructure.ItemRepository;
import com.C_platform.payment.application.port.PaymentGatewayPort;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final Map<String, PaymentGatewayPort> gateways;
    private final OrderRepository orderRepository;
    private final ItemPricingReader itemReader;
    private final ItemRepository itemRepository;

    /**
     * 결제 요청(ready): PG별로 라우팅하여 redirect_url 반환
     */
    @Transactional
    public AttemptPaymentResponse ready(AttemptPaymentRequest req, Long currentUserId) {
        try {
            String key = normalize(req.paymentMethod()); // "KAKAOPAY" | "NAVERPAY"
            PaymentGatewayPort gateway = gateways.get(key);
            if (gateway == null) {
                throw new PaymentException(PaymentErrorCode.P003); // payment.order.not.owner (미지원 결제수단)
            }

            AttemptPaymentResponse resp = gateway.ready(req, currentUserId);
            return resp;

        } catch (PaymentException e) {
            log.warn("결제 요청 실패: {}", e.getErrorCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("PG 초기화 중 예외 발생", e);
            throw new PaymentException(PaymentErrorCode.P004, e); // payment.pg.init.failed
        }
    }

    /**
     * 결제 완료(approve/cancel/fail): PG별로 라우팅하여 승인/취소 처리
     */
    @Transactional
    public CompletePaymentResponse complete(
            Long orderId,
            CompletePaymentRequest req,
            Long currentUserId
    ) {
        try {
            String key = normalize(req.provider());
            PaymentGatewayPort gateway = gateways.get(key);
            if (gateway == null) {
                throw new PaymentException(PaymentErrorCode.P003); // 미지원 PG사
            }

            // ✅ PG 승인 처리
            CompletePaymentResponse response = gateway.complete(orderId, req, currentUserId);

            // ✅ 주문 조회
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.P002)); // payment.order.not.found

            // ✅ 아이템 조회
            Item item = itemRepository.findById(order.getItemSnapshot().getItemId())
                    .orElseThrow(() -> new PaymentException(PaymentErrorCode.P005)); // payment.request.invalid (상품 누락)

            // ✅ 결제 성공 → 상품 SOLD_OUT 처리
            try {
                item.placeOrder(order);
            } catch (IllegalStateException ex) {
                // 이미 판매된 상품인 경우
                log.warn("상품 SOLD_OUT 상태 감지: itemId={}", item.getId());
                throw new PaymentException(PaymentErrorCode.P008); // payment.approve.cancel
            }

            return response;

        } catch (PaymentException e) {
            log.warn("결제 완료 처리 중 비즈니스 예외: {}", e.getErrorCode().getMessage());
            throw e;
        } catch (Exception e) {
            log.error("결제 승인 처리 중 예외 발생", e);
            throw new PaymentException(PaymentErrorCode.P009, e); // payment.approve.fail
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }
}

