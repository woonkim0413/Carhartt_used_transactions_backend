package com.C_platform.Member.domain.Oauth;

public class OAuthRegistration {
    private final OAuthProvider provider;
    private final String userInfoUri;

    public OAuthRegistration(OAuthProvider provider, String userInfoUri) {
        this.provider = provider;
        // 해당 필드는 Resource Server에서 사용자 정보를 가져오기 위해 사용되는 End-Point를 저장
        // (Kakao Oauth 페이지 호출 url X, Kakao Oauth 인증 완료 후 redirect url X, Authorization code를 Access-Token으로 바꾸기 위해 kakao Authorization Server에 보내는 url X)

        // Naver :
        // Kakao : https://kapi.kakao.com/v2/user/me
        this.userInfoUri = userInfoUri;
    }

    public String getUserInfoUri() {
        return userInfoUri;
    }

    public OAuthProvider getProvider() {
        return provider;
    }

    // static factory
    public static OAuthRegistration kakao() {
        return new OAuthRegistration(OAuthProvider.KAKAO, "https://kapi.kakao.com/v2/user/me");
    }

    public static OAuthRegistration naver() {
        return new OAuthRegistration(OAuthProvider.NAVER, "https://openapi.naver.com/v1/nid/me");
    }
}
