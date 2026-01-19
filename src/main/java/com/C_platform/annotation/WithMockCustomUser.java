package com.C_platform.annotation;

import com.C_platform.Member_woonkim.domain.enums.LocalProvider;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.security.test.context.support.WithSecurityContext;

// test 할 때 인증 securityContext를 생성하여 스레드 로컬에 넣어주는 class
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@WithSecurityContext(factory = WithMockCustomUserSecurityContextFactory.class)
public @interface WithMockCustomUser {

    String email() default "test@example.com";

    String password() default "password123";

    long memberId() default 1L;

    String name() default "테스트유저";

    LocalProvider provider() default LocalProvider.LOCAL;

    String[] roles() default {"ROLE_USER"};

    boolean accountExpired() default false;

    boolean accountLocked() default false;

    boolean credentialsExpired() default false;

    boolean disabled() default false;
}
