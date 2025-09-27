package com.C_platform.Member.domain.Oauth;

import com.C_platform.Member.domain.Member.Member;

public record JoinOrLoginResult(Member member, boolean isNew) {}
