package com.C_platform.payment.application.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Objects;

public record KakaoAttemptRequest(
        String cid,
        @JsonProperty("partner_order_id") String partnerOrderId,
        @JsonProperty("partner_user_id") String partnerUserId,
        @JsonProperty("item_name")        String itemName,
        Integer quantity,
        @JsonProperty("total_amount")     Integer totalAmount,
        @JsonProperty("tax_free_amount")  Integer taxFreeAmount,
        @JsonProperty("approval_url")     String approvalUrl,
        @JsonProperty("cancel_url")       String cancelUrl,
        @JsonProperty("fail_url")         String failUrl
) {
    public MultiValueMap<String, String> toForm() {
        // --- 강제 검증(필수/범위) ---
        String _cid = mustText(cid, "cid");
        String _orderId = mustText(partnerOrderId, "partner_order_id");
        String _userId = mustText(partnerUserId, "partner_user_id");
        String _item = mustText(itemName, "item_name");
        int _qty = mustPositive(quantity, "quantity");
        int _amount = mustPositive(totalAmount, "total_amount");
        int _taxFree = (taxFreeAmount == null ? 0 : nonNegative(taxFreeAmount, "tax_free_amount"));
        String _approve = mustText(approvalUrl, "approval_url");
        String _cancel  = mustText(cancelUrl, "cancel_url");
        String _fail    = mustText(failUrl, "fail_url");

        // --- x-www-form-urlencoded payload ---
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("cid", _cid);
        form.add("partner_order_id", _orderId);
        form.add("partner_user_id", _userId);
        form.add("item_name", _item);
        form.add("quantity", Integer.toString(_qty));
        form.add("total_amount", Integer.toString(_amount));
        form.add("tax_free_amount", Integer.toString(_taxFree));
        form.add("approval_url", _approve);
        form.add("cancel_url", _cancel);
        form.add("fail_url", _fail);
        return form;
    }

    private static String mustText(String v, String name) {
        if (v == null || v.isBlank()) throw new IllegalArgumentException(name + " required");
        return v.trim();
    }
    private static int mustPositive(Integer v, String name) {
        if (v == null || v < 1) throw new IllegalArgumentException(name + " must be >= 1");
        return v;
    }
    private static int nonNegative(Integer v, String name) {
        if (v < 0) throw new IllegalArgumentException(name + " must be >= 0");
        return v;
    }
}
