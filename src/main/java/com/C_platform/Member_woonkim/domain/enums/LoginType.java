package com.C_platform.Member_woonkim.domain.enums;

import lombok.Getter;

// CUSTOM은 spring login 기능을 이용하여 직접 login을 구현하는 방법
@Getter
public enum LoginType {
    LOCAL("LOCAL"),
    OAUTH("OAUTH");

    private String LoginType;

    LoginType(String loginType) {
        this.LoginType = loginType;
    }
}
