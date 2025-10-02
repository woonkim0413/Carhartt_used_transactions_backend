package com.C_platform.Member_woonkim.domain.member_enum;

import com.C_platform.Member_woonkim.domain.member_interface.Provider;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum LocalProvider implements Provider {
    LOCAL("LOCAL");

    private final String providerName;
}
