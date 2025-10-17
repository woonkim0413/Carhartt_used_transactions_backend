package com.C_platform.payment.infrastructure.adapter;

import com.C_platform.order.domain.ItemSnapshot;
import com.C_platform.order.domain.Order;
import com.C_platform.order.domain.OrderRepository;
import com.C_platform.payment.application.dto.naver.NaverAttemptRequest;
import com.C_platform.payment.application.dto.naver.NaverAttemptResponse;
import com.C_platform.payment.application.dto.naver.NaverCompleteRequest;
import com.C_platform.payment.application.dto.naver.NaverCompleteResponse;
import com.C_platform.payment.application.port.PaymentGatewayPort;
import com.C_platform.payment.domain.Payment;
import com.C_platform.payment.domain.PaymentMethod;
import com.C_platform.payment.domain.PaymentStatus;
import com.C_platform.payment.infrastructure.PaymentRepository;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component("NAVERPAY")
public class NaverPayAdapter implements PaymentGatewayPort {

    private final WebClient naverPayWebClient;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public NaverPayAdapter(
            @Qualifier("naverPayWebClient") WebClient naverPayWebClient,  // ← 생성자 파라미터에!
            OrderRepository orderRepository,
            PaymentRepository paymentRepository
    ) {
        this.naverPayWebClient = naverPayWebClient;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    // === 1️⃣ READY 단계 ===
    @Override
    @Transactional
    public AttemptPaymentResponse ready(AttemptPaymentRequest req, Long currentUserId) {
        // 주문 조회
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + req.orderId()));
        ItemSnapshot snapshot = order.getItemSnapshot();

        // 결제 금액
        int totalAmount = snapshot.getPrice().intValueExact();
        String itemName = "ITEM-" + snapshot.getItemId();

        // 네이버 reserve 요청 조립
// 네이버 reserve 요청 조립
        var nReq = NaverAttemptRequest.from(
                "ORDER-" + order.getId(),  // userKey → orderKey로 변경
                itemName,
                totalAmount,
                req.returnUrl()
        );
        System.out.println("=== 네이버페이 API 호출 시작 ===");
        System.out.println("=== Request: " + nReq);

        // API 호출
        NaverAttemptResponse nRes = naverPayWebClient.post()
                .uri("/v2.2/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(nReq))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(error -> {
                                    System.err.println("=== NaverPay API Error Response ===");
                                    System.err.println(error);  // ← 에러 상세 출력
                                })
                                .flatMap(error -> Mono.error(new IllegalStateException("Naver API error: " + error)))
                )
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(error -> System.err.println("=== NaverPay Error: " + error))
                                .flatMap(error -> Mono.error(new IllegalStateException("Naver API error: " + error)))
                )
                .bodyToMono(NaverAttemptResponse.class)
                .doOnSuccess(res -> {
                    if (res != null) {
                        System.out.println("=== SUCCESS reserveId: " + res.reserveId());
                        System.out.println("=== redirectUrl: " + res.paymentUrl());
                    }
                })
                .block();

        if (nRes == null || nRes.reserveId() == null) {
            throw new IllegalStateException("Naver reserve failed: reserveId is missing");
        }

        // Payment 생성 및 PENDING 상태로 전이
        Payment payment = Payment.newReady(order, PaymentMethod.NAVERPAY, totalAmount);
        payment.markPending(nRes.reserveId());
        paymentRepository.save(payment);

        // FE로 리다이렉트 URL 반환
        return new AttemptPaymentResponse(
                null, null,
                nRes.paymentUrl().pc(),
                null, null
        );
    }

    // === 2️⃣ APPROVE 단계 ===
    @Override
    @Transactional
    public CompletePaymentResponse complete(CompletePaymentRequest req, Long currentUserId) {
        if (!"NAVERPAY".equalsIgnoreCase(req.provider())) {
            throw new IllegalArgumentException("provider mismatch (expected NAVERPAY): " + req.provider());
        }

        // 주문 조회
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + req.orderId()));

        Payment payment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("payment not found by orderId: " + order.getId()));

        String reserveId = payment.getTransactionId();
        if (reserveId == null) {
            throw new IllegalStateException("missing reserveId for orderId=" + order.getId());
        }

        // FE에서 받은 paymentId (pgToken 필드에 담겨옴)
        String paymentId = req.pgToken();
        var approveReq = new NaverCompleteRequest(paymentId);

        // API 호출
        NaverCompleteResponse nRes = naverPayWebClient.post()
                .uri("/v2.2/apply/payment")
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(approveReq))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(error -> System.err.println("=== NaverPay Approve Error: " + error))
                                .flatMap(error -> Mono.error(new IllegalStateException("Naver API error: " + error)))
                )
                .bodyToMono(NaverCompleteResponse.class)
                .block();

        // 응답 검증
        if (nRes == null || nRes.body() == null) {
            throw new IllegalStateException("Naver approve failed: null response");
        }

        if (nRes.body().totalPayAmount() != payment.getAmountOfPayment()) {
            throw new IllegalStateException("amount mismatch");
        }

        // 상태 전이
        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            payment.approve();
            paymentRepository.save(payment);
        }

        order.makePaid();
        orderRepository.save(order);

        return new CompletePaymentResponse(order.getId());
    }
}

