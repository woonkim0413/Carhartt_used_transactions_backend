package com.C_platform.Member_woonkim.domain.dto;

import com.C_platform.Member_woonkim.domain.entitys.Member;

public record JoinOrLoginResult(Member member, boolean isNew) {}
