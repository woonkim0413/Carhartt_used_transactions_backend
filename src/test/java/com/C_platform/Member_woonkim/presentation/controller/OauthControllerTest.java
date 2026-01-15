package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.OAuth2UseCase;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.value.CustomOAuth2User;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * OauthController의 통합 테스트 클래스
 * - GET /v1/oauth/login: 로그인 방식 목록 조회
 * - GET /v1/oauth/login/check: 로그인 상태 확인
 */
@SpringBootTest(properties = {
        "spring.session.store-type=none",
        "spring.data.redis.repositories.enabled=false"
})
@AutoConfigureMockMvc
@Transactional
class OauthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private OAuth2UseCase oauth2UseCase;

    private Member testMember;
    private CustomOAuth2User mockOAuth2User;

    @BeforeEach
    void setUp() {
        // 테스트용 OAuth 회원 생성 및 저장
        testMember = new Member(
                OAuthProvider.KAKAO,
                "12345678",
                "테스트유저",
                "test@kakao.com"
        );
        memberRepository.save(testMember);

        // Mock CustomOAuth2User 생성
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", testMember.getMemberId());
        attributes.put("memberName", testMember.getName());
        attributes.put("oauthId", testMember.getOauthId());
        attributes.put("memberNickname", testMember.getNickname());
        attributes.put("email", testMember.getEmail());
        attributes.put("loginType", LoginType.OAUTH.getLoginType());
        attributes.put("provider", testMember.getOauthProvider());

        mockOAuth2User = new CustomOAuth2User(
                testMember.getMemberId(),
                attributes,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
    }

    /**
     * 테스트 1: 로그인 방식 목록 조회 API - 정상 케이스
     */
    @Test
    @DisplayName("정상적인 로그인 방식 목록 조회 요청 시 목록과 200 OK 반환")
    void getLoginProviders_Success() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/oauth/login")
                        .header("X-Request-Id", "test-req-001"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(greaterThan(0))) // 최소 1개 이상의 로그인 방식
                .andExpect(jsonPath("$.data[*].type").exists())
                .andExpect(jsonPath("$.data[*].provider").exists())
                .andExpect(jsonPath("$.meta.x_request_id").value("test-req-001"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    /**
     * 테스트 2: 로그인 방식 목록 조회 API - X-Request-Id 없는 경우
     */
    @Test
    @DisplayName("X-Request-Id 헤더 없이 로그인 방식 목록 조회 시 200 OK 반환")
    void getLoginProviders_WithoutRequestId() throws Exception {
        // when & then (X-Request-Id 없이 요청)
        mockMvc.perform(get("/v1/oauth/login"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray());
    }

    /**
     * 테스트 3: 로그인 방식 목록 조회 API - 반환 데이터 구조 검증
     */
    @Test
    @DisplayName("로그인 방식 목록의 데이터 구조가 올바른지 검증")
    void getLoginProviders_DataStructure() throws Exception {
        // when & then
        mockMvc.perform(get("/v1/oauth/login"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].type").isString())
                .andExpect(jsonPath("$.data[0].provider").isString())
                .andExpect(jsonPath("$.data[0].authorize_url").exists());
    }

    /**
     * 테스트 4: 로그인 확인 API - 정상 케이스 (OAuth 로그인)
     * NOTE: Redis 세션 저장소가 필요하여 현재 비활성화됨
     */
    @Test
    @Disabled("Redis 세션 저장소 필요")
    @DisplayName("정상적인 OAuth 로그인 확인 요청 시 사용자 정보와 200 OK 반환")
    void oauthLoginCheck_Success() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockOAuth2User, null, mockOAuth2User.getAuthorities());

        // when & then
        mockMvc.perform(get("/v1/oauth/login/check")
                        .with(authentication(auth))
                        .header("X-Request-Id", "test-req-002"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.member_id").value(testMember.getMemberId().toString()))
                .andExpect(jsonPath("$.data.member_name").value(testMember.getName()))
                .andExpect(jsonPath("$.data.member_nickname").value(testMember.getNickname()))
                .andExpect(jsonPath("$.data.email").value(testMember.getEmail()))
                .andExpect(jsonPath("$.data.login_type").value("OAUTH"))
                .andExpect(jsonPath("$.data.provider").value(testMember.getOauthProvider().getProviderName()))
                .andExpect(jsonPath("$.meta.x_request_id").value("test-req-002"))
                .andExpect(jsonPath("$.meta.timestamp").exists());
    }

    /**
     * 테스트 5: 로그인 확인 API - 인증되지 않은 사용자
     */
    @Test
    @DisplayName("인증되지 않은 사용자의 로그인 확인 요청 시 에러 반환")
    void oauthLoginCheck_Unauthorized() throws Exception {
        // when & then (인증 정보 없이 요청)
        mockMvc.perform(get("/v1/oauth/login/check")
                        .header("X-Request-Id", "test-req-003"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("C001")); // OauthErrorCode.C001 (로그인 상태가 아닙니다)
    }

    /**
     * 테스트 6: 로그인 확인 API - 프로필 이미지 URL 포함 검증
     * NOTE: Redis 세션 저장소가 필요하여 현재 비활성화됨
     */
    @Test
    @Disabled("Redis 세션 저장소 필요")
    @DisplayName("로그인 확인 시 프로필 이미지 URL이 응답에 포함되는지 검증")
    void oauthLoginCheck_WithProfileImage() throws Exception {
        // given: 프로필 이미지 URL이 있는 회원
        testMember.changeProfileImage("https://example.com/profile.jpg");
        memberRepository.save(testMember);

        // Mock CustomOAuth2User 재생성 (업데이트된 정보 반영)
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", testMember.getMemberId());
        attributes.put("memberName", testMember.getName());
        attributes.put("oauthId", testMember.getOauthId());
        attributes.put("memberNickname", testMember.getNickname());
        attributes.put("email", testMember.getEmail());
        attributes.put("loginType", LoginType.OAUTH.getLoginType());
        attributes.put("provider", testMember.getOauthProvider());

        CustomOAuth2User updatedMockUser = new CustomOAuth2User(
                testMember.getMemberId(),
                attributes,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(updatedMockUser, null, updatedMockUser.getAuthorities());

        // when & then
        mockMvc.perform(get("/v1/oauth/login/check")
                        .with(authentication(auth)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.profile_image_url").value("https://example.com/profile.jpg"));
    }

    /**
     * 테스트 7: 로그인 확인 API - X-Request-Id 없는 경우
     * NOTE: Redis 세션 저장소가 필요하여 현재 비활성화됨
     */
    @Test
    @Disabled("Redis 세션 저장소 필요")
    @DisplayName("X-Request-Id 헤더 없이 로그인 확인 시 200 OK 반환")
    void oauthLoginCheck_WithoutRequestId() throws Exception {
        // given
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockOAuth2User, null, mockOAuth2User.getAuthorities());

        // when & then (X-Request-Id 없이 요청)
        mockMvc.perform(get("/v1/oauth/login/check")
                        .with(authentication(auth)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.member_id").value(testMember.getMemberId().toString()));
    }

    /**
     * 테스트 8: 로그인 확인 API - Naver OAuth 회원
     * NOTE: Redis 세션 저장소가 필요하여 현재 비활성화됨
     */
    @Test
    @Disabled("Redis 세션 저장소 필요")
    @DisplayName("Naver OAuth 로그인 회원의 로그인 확인 성공")
    void oauthLoginCheck_NaverProvider() throws Exception {
        // given: Naver OAuth 회원 생성
        Member naverMember = new Member(
                OAuthProvider.NAVER,
                "naver12345",
                "네이버유저",
                "test@naver.com"
        );
        memberRepository.save(naverMember);

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("memberId", naverMember.getMemberId());
        attributes.put("memberName", naverMember.getName());
        attributes.put("oauthId", naverMember.getOauthId());
        attributes.put("memberNickname", naverMember.getNickname());
        attributes.put("email", naverMember.getEmail());
        attributes.put("loginType", LoginType.OAUTH.getLoginType());
        attributes.put("provider", naverMember.getOauthProvider());

        CustomOAuth2User naverOAuth2User = new CustomOAuth2User(
                naverMember.getMemberId(),
                attributes,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(naverOAuth2User, null, naverOAuth2User.getAuthorities());

        // when & then
        mockMvc.perform(get("/v1/oauth/login/check")
                        .with(authentication(auth)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.provider").value("NAVER"))
                .andExpect(jsonPath("$.data.login_type").value("OAUTH"));
    }
}
