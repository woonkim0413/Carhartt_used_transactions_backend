package com.C_platform.Member_woonkim.domain.Oauth;

import com.C_platform.Member_woonkim.domain.member_entity.Member;

public record JoinOrLoginResult(Member member, boolean isNew) {}
