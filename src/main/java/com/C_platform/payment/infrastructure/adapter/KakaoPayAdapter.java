package com.C_platform.payment.infrastructure.adapter;

import com.C_platform.order.domain.ItemSnapshot;
import com.C_platform.order.domain.Order;
import com.C_platform.order.domain.OrderRepository;
import com.C_platform.payment.application.dto.kakao.KakaoAttemptRequest;
import com.C_platform.payment.application.dto.kakao.KakaoAttemptResponse;
import com.C_platform.payment.application.dto.kakao.KakaoCompleteRequest;
import com.C_platform.payment.application.dto.kakao.KakaoCompleteResponse;
import com.C_platform.payment.application.port.PaymentGatewayPort;
import com.C_platform.payment.domain.Payment;
import com.C_platform.payment.domain.PaymentMethod;
import com.C_platform.payment.domain.PaymentStatus;
import com.C_platform.payment.infrastructure.PaymentRepository;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component("KAKAOPAY")
public class KakaoPayAdapter implements PaymentGatewayPort  {

    private final WebClient kakaoPayWebClient;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    private final String cid;

    public KakaoPayAdapter(
            WebClient kakaoPayWebClient,
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            @Value("${pay.kakao.cid:TC0ONETIME}") String cid
    ) {
        this.kakaoPayWebClient = kakaoPayWebClient;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.cid = cid;
    }

    /**
     * FE → 결제 시도: AttemptPaymentRequest 기반으로 카카오 ready 호출
     */
    /**
     * FE → 결제 시도: AttemptPaymentRequest 기반으로 카카오 ready 호출
     */
    @Override
    @Transactional
    public  AttemptPaymentResponse ready(AttemptPaymentRequest req, Long currentUserId) {
        // 1) 주문/스냅샷 조회
        Order order = orderRepository.findById(req.orderId())
                .orElseThrow(() -> new IllegalArgumentException("order not found: " + req.orderId()));
        ItemSnapshot snapshot = order.getItemSnapshot();

        // 2) 금액/항목명/수량 (중고거래: 1 고정)
        int totalAmount = snapshot.getPrice();
        String itemName = "ITEM-" + snapshot.getItemId();
        int quantity = 1;
        int taxFreeAmount = 0;

        // 3) 카카오 ready 요청 조립
        var kReq = new KakaoAttemptRequest(
                cid,
                order.getId().toString(),           // partner_order_id
                currentUserId.toString(),           // partner_user_id
                itemName,
                quantity,
                totalAmount,
                taxFreeAmount, //tax_free_amount
                req.approveUrl(),
                req.cancelUrl(),
                req.failUrl()
        );

        // 디버깅: 전송되는 데이터 확인
        System.out.println("=== CID: " + cid);

        // 4) 호출
        KakaoAttemptResponse kRes = kakaoPayWebClient.post()
                .uri("/payment/ready") // base-url: https://open-api.kakaopay.com/online/v1
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(kReq))
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        response -> response.bodyToMono(String.class)
                                .doOnNext(errorBody -> System.err.println("=== 카카오페이 에러: " + errorBody))
                                .flatMap(errorBody -> Mono.error(new IllegalStateException("Kakao API error: " + errorBody)))
                )
                .bodyToMono(KakaoAttemptResponse.class)
                .doOnSuccess(res -> {
                    System.out.println("=== SUCCESS Response TID: " + res.tid());
                    System.out.println("=== SUCCESS Response PC URL: " + res.nextRedirectPcUrl());
                })
                .block();

        // 5) 응답 검증
        if (kRes == null || kRes.tid() == null) {
            throw new IllegalStateException("Kakao ready failed: tid is missing in response.");
        }

        // 6) Payment 생성 및 PENDING 상태 전이 (저장 로직 통합)
        // 두 번의 save() 대신, Payment 객체를 생성/수정 후 한 번만 저장하도록 단순화
        Payment payment = Payment.newReady(order, PaymentMethod.KAKAOPAY, totalAmount);
        payment.markPending(kRes.tid()); // transactionId=tid, status=PENDING
        paymentRepository.save(payment);

        // 7) FE에 필요한 리다이렉트 정보만 UI DTO로 변환하여 반환
        return new AttemptPaymentResponse(
                kRes.nextRedirectAppUrl(),
                kRes.nextRedirectMobileUrl(),
                kRes.nextRedirectPcUrl(),
                kRes.androidAppScheme(),
                kRes.iosAppScheme()
        );
    }

    /**
     * PG 콜백 처리(승인/실패/취소): CompletePaymentRequest 기반
     * - provider: "KAKAOPAY"
     * - merchant_uid: 가맹점 주문 식별자(컨벤션에 맞춰 Order를 찾을 수 있어야 함)
     * - result_hint: SUCCESS | FAIL | CANCEL
     * - pg_token: SUCCESS일 때만 필수
     */
    @Transactional
    public CompletePaymentResponse complete(
            Long orderId,  // ✅ orderId 파라미터 추가
            CompletePaymentRequest req,
            Long currentUserId
    ) {
        if (!"KAKAOPAY".equalsIgnoreCase(req.provider())) {
            throw new IllegalArgumentException("provider mismatch (expected KAKAOPAY): " + req.provider());
        }

        // ── [주문/결제 찾기] ─────────────────────────────────────────
        // merchantUid → Order 로 resolve 하는 방식은 너 스키마에 맞춰 구현.
        // 1) 만약 Order에 merchantUid 필드가 있다면:
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("order not found by merchantUid: " + orderId));

        // 2) Payment는 Order 기준으로 잠금 조회
        var payment = paymentRepository.findByOrderIdForUpdate(order.getId())
                .orElseThrow(() -> new IllegalArgumentException("payment not found by orderId: " + order.getId()));


        // ── [승인 처리] ────────────────────────────────────────────
        // ready 때 저장했던 카카오 tid(transactionId)을 꺼낸다.
        String tid = payment.getTransactionId();
        if (tid == null) {
            throw new IllegalStateException("missing tid for orderId=" + order.getId());
        }
        if (req.pgToken() == null || req.pgToken().isBlank()) {
            throw new IllegalArgumentException("pg_token is required for SUCCESS");
        }

        // 카카오 approve 요청
        var approveReq = new KakaoCompleteRequest(
                cid,
                tid,
                order.getId().toString(),     // partner_order_id
                currentUserId.toString(),     // partner_user_id
                req.pgToken()
        );

        KakaoCompleteResponse aRes = kakaoPayWebClient.post()
                .uri("/payment/approve")
                // 🚨 2. Content-Type을 JSON으로 변경
                .contentType(MediaType.APPLICATION_JSON)
                // 🚨 3. DTO 객체를 Body로 전송
                .body(BodyInserters.fromValue(approveReq))
                .retrieve()
                .bodyToMono(KakaoCompleteResponse.class)
                .block();

        if (aRes == null || aRes.amount() == null) {
            throw new IllegalStateException("kakao approve failed");
        }

        // 금액 동등성 검증
        Integer approvedTotal = aRes.amount().total();
        if (approvedTotal == null || approvedTotal != payment.getAmountOfPayment()) {
            throw new IllegalStateException("amount mismatch: approved=" + approvedTotal
                    + ", saved=" + payment.getAmountOfPayment());
        }

        // 상태 전이
        if (payment.getPaymentStatus() != PaymentStatus.PAID) {
            payment.approve();
            paymentRepository.save(payment);
        }

        order.makePaid(); // OrderStatus를 PAID로 변경하는 메서드 호출
        orderRepository.save(order);

        return new CompletePaymentResponse(order.getId());
    }
}

