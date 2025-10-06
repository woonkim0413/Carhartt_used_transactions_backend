package com.C_platform.payment.application;

import com.C_platform.exception.PaymentException;
import com.C_platform.global.error.PaymentErrorCode;
import com.C_platform.payment.application.port.PaymentGatewayPort;
import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final Map<String, PaymentGatewayPort> gateways;

    /**
     * 결제 요청(ready): PG별로 라우팅하여 redirect_url 반환
     */
    @Transactional
    public AttemptPaymentResponse ready(AttemptPaymentRequest req, Long currentUserId) {
        try {
            String key = normalize(req.paymentMethod()); // "KAKAOPAY" | "NAVERPAY"
            PaymentGatewayPort gateway = gateways.get(key);
            if (gateway == null) throw new PaymentException(PaymentErrorCode.P003); // UNSUPPORTED_METHOD 등

            // 🚨 String으로 받지 않고, AttemptPaymentResponse 객체 전체를 받습니다.
            AttemptPaymentResponse resp = gateway.ready(req, currentUserId);

            // 🚨 받은 객체를 그대로 반환합니다.
            return resp;

        } catch (PaymentException e) {
            // Adapter/도메인에서 던진 명시적 에러는 그대로 전파
            throw e;
        } catch (Exception e) {
            // 알 수 없는 오류는 PG 초기화 실패로 래핑
            throw new PaymentException(PaymentErrorCode.P004, e); // PG_INIT_FAILED
        }
    }

    /**
     * 결제 완료(approve/cancel/fail): PG별로 라우팅하여 승인/취소 처리
     */
    @Transactional
    public CompletePaymentResponse complete(CompletePaymentRequest req, Long currentUserId) {
        try {
            String key = normalize(req.provider()); // "KAKAOPAY" | "NAVERPAY"
            PaymentGatewayPort gateway = gateways.get(key);
            if (gateway == null) throw new PaymentException(PaymentErrorCode.P003); // UNSUPPORTED_PROVIDER 등

            return gateway.complete(req, currentUserId);

        } catch (PaymentException e) {
            throw e;
        } catch (Exception e) {
            // 승인 단계의 일반 예외는 승인 거절 계열로 래핑
            throw new PaymentException(PaymentErrorCode.P009, e); // APPROVE_REJECTED 등 팀 코드에 맞춰 사용
        }
    }

    private String normalize(String s) {
        if (s == null) return "";
        return s.trim().toUpperCase();
    }
}
