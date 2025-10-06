package com.C_platform.payment.application.port;

import com.C_platform.payment.ui.dto.AttemptPaymentRequest;
import com.C_platform.payment.ui.dto.AttemptPaymentResponse;
import com.C_platform.payment.ui.dto.CompletePaymentRequest;
import com.C_platform.payment.ui.dto.CompletePaymentResponse;

// com.C_platform.payment.application.port
public interface PaymentGatewayPort {
    /** ready/reserve 호출 후, PG 결제창 redirect URL 반환 */
    AttemptPaymentResponse ready(AttemptPaymentRequest req, Long currentUserId);

    /** 승인(approve) 처리 */
    CompletePaymentResponse complete(CompletePaymentRequest req, Long currentUserId);
}



