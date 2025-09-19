package com.C_platform.Member.ui.controller;

import com.C_platform.Member.domain.Oauth.MemberErrorCode;
import com.C_platform.Member.domain.Oauth.OAuth2KakaoUserInfoDto;
import com.C_platform.Member.domain.Oauth.OAuth2Service;
import com.C_platform.Member.domain.Oauth.OAuthProvider;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.global.error.ErrorBody;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OauthController {

    private final OAuth2Service oauth2Service;

    // Kakao/Naver 로그인 공급자 목록 반환
    @GetMapping("/auth/login")
    public ApiResponse<Map<String, Object>> getLoginProviders() {
        String requestId = UUID.randomUUID().toString();

        List<Map<String, Object>> providers = List.of(
                Map.of(
                        "provider", "kakao",
                        "display_name", "카카오",
                        "type", "oauth",
                        "authorize_url", oauth2Service.getAuthorizeUrl(OAuthProvider.KAKAO)
                ),
                Map.of(
                        "provider", "naver",
                        "display_name", "네이버",
                        "type", "oauth",
                        "authorize_url", oauth2Service.getAuthorizeUrl(OAuthProvider.NAVER)
                )
        );
        return ApiResponse.success(
                Map.of("providers", providers),
                MetaData.builder().timestamp(LocalDateTime.now()).build()
        );
    }

    // 2. 카카오 로그인 리다이렉트
    @GetMapping("/auth/login/kakao")
    public void redirectToKakao(HttpServletResponse response) throws IOException {
        String authorizeUrl = oauth2Service.getAuthorizeUrl(OAuthProvider.KAKAO);
        response.sendRedirect(authorizeUrl);
    }

    // 3. 카카오 콜백 처리 - 사용자 정보 session 저장
    @GetMapping("/oauth/kakao/callback")
    public void kakaoCallback(
            @RequestParam("code") String code,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {
        // todo 실패하는 경우에 대한 response 코드 작성 필요
        if (code == null || code.isBlank()) {
            response.sendRedirect("/login?error=missing_code");
            return;
        }
        // 1. Access Token 요청
        String accessToken = oauth2Service.getAccessToken(code, OAuthProvider.KAKAO);

        // 2. 사용자 정보 획득
        OAuth2KakaoUserInfoDto userInfo = oauth2Service.getUserInfo(accessToken, OAuthProvider.KAKAO);

        // 3. 세션에 저장
        session.setAttribute("user", userInfo);

        // 4. 클라이언트 홈으로 리다이렉트
        response.sendRedirect("http://localhost:3000");  // 프론트 홈 경로로 수정 가능
    }

    @PostMapping("/v1/auth/logout")
    public ApiResponse<?> logout(HttpSession session) {
        session.invalidate();
        return ApiResponse.success(Map.of("message", "로그아웃 완료"), MetaData.builder().timestamp(LocalDateTime.now()).build());
    }

    @GetMapping("/v1/auth/me")
    public ApiResponse<?> getMyInfo(HttpSession session) {
        OAuth2KakaoUserInfoDto user = (OAuth2KakaoUserInfoDto) session.getAttribute("user");

        if (user == null) {
            return ApiResponse.fail(
                    new ErrorBody<>(MemberErrorCode.O001),  // 예: 미인증
                    MetaData.builder().timestamp(LocalDateTime.now()).build()
            );
        }

        return ApiResponse.success(
                Map.of(
                        "id", user.getId(),
                        "nickname", user.getName()
                ),
                MetaData.builder().timestamp(LocalDateTime.now()).build()
        );
    }
}
