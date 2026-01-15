package com.C_platform.Member_woonkim.domain.value;

import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

/**
 * Local login시에 AuthenticationToken에 저장할 Principal 객체
 **/
@Getter
public class CustomLocalUser extends User {

    private final Long memberId;
    private final String email;
    private final LocalProvider localProvider;
    // todo 닉네임도 저장하기 (Principal 객체에서 바로 꺼내기 위해)

    /**
     * Lombok @Builder가 붙는 생성자
     * - 이 생성자의 파라미터 목록이 그대로 builder()의 필드가 됨
    **/
    @Builder(builderMethodName = "customBuilder")
    public CustomLocalUser(
            String username,
            String password,
            Collection<? extends GrantedAuthority> authorities,
            boolean accountExpired,
            boolean accountLocked,
            boolean credentialsExpired,
            boolean disabled,
            Long memberId,
            String email,
            LocalProvider localProvider
    ) {
        // 부모 생성자 호출
        super(username, password, !disabled, !accountExpired, !credentialsExpired, !accountLocked, authorities);

        // principal에 추가로 저장할 정보들
        this.memberId = memberId;
        this.email = email;
        this.localProvider = localProvider;
    }
}
