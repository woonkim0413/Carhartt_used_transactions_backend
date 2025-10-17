package com.C_platform.Member_woonkim.infrastructure.register_and_userInfoParsers;

import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;

import java.util.Map;

public interface OAuth2UserInfoParser {

    Provider getProvider();

    OAuth2UserInfoDto parse(Map<String,Object> userInfo);
}
