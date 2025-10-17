package com.C_platform.Member_woonkim.infrastructure.register_and_userInfoParsers;

import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
// userInfo에서 로그인 타입에 따른 Parser을 반환 해주는 클래스이다
public class OAuthUserInfoParserRegister {
    private final Map<Provider, OAuth2UserInfoParser> byProvider;

    // (중요)
    // ParserRegistry가 bean으로 등록되어 있기에 spring이 bean으로 등록된 모든
    // UserInfoParser들을 가져와서 List로 묶은 뒤 arg로 넣어준다
    public OAuthUserInfoParserRegister(List<OAuth2UserInfoParser> parsers) {
        // parsers를 stream을 이용하여 immutable Map으로 만드는 코드다
        // toUnmodifiableMap(UserInfoParser::provider, p -> p)에서
        // UserInfoParser::getProvider는 Map의 key, p -> p는 Map의 vaule가 된다
        this.byProvider = parsers.stream()
                .collect(Collectors.toUnmodifiableMap(OAuth2UserInfoParser::getProvider, p -> p));
    }

    public OAuth2UserInfoParser of(Provider provider) {
        var p = byProvider.get(provider);
        if (p == null) throw new UnsupportedOperationException("Unsupported provider: " + provider);
        return p;
    }
}
