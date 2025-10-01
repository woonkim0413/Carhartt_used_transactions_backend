package com.C_platform.Member.domain.Oauth;

import java.util.Map;

public interface UserInfoParser {
    Provider getProvider();
    OAuth2UserInfoDto parse(Map<String,Object> userInfo);
}
