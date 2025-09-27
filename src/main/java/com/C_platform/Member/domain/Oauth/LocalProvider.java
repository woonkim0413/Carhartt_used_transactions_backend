package com.C_platform.Member.domain.Oauth;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.Serializable;

@Getter
@AllArgsConstructor
public enum LocalProvider implements Provider {
    LOCAL("LOCAL");

    private final String providerName;
}
