package com.C_platform.Member.ui.controller;

import com.C_platform.Member.domain.Member.Member;
import com.C_platform.Member.domain.Oauth.*;
import com.C_platform.Member.domain.exception.KakaoOauthErrorCode;
import com.C_platform.Member.domain.exception.KakaoOauthException;
import com.C_platform.Member.ui.dto.CallBackResponseDto;
import com.C_platform.Member.ui.dto.KakaoCallbackRequestDto;
import com.C_platform.Member.ui.dto.LoginProviderDto;
import com.C_platform.Member.ui.dto.LogoutRequestDto;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.global.logPaint;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/v1")
@RequiredArgsConstructor
public class OauthController {

    private final OAuth2Service oauth2Service;
    private final MemberJoinService memberJoinService;

    // Kakao/Naver 로그인 공급자 목록 반환
    @GetMapping("/oauth/login")
    @Operation(summary = "로그인 방식 (Oauths, local) 목록 출력", description = " 서비스가 지원하는 로그인 방식을 조회 합니다.")
    public ResponseEntity<ApiResponse<List<LoginProviderDto>>> getLoginProviders() {
        String requestId = UUID.randomUUID().toString();

        logPaint.sep("로그인 방식 목록 호출 진입");
        // meta 생성
        MetaData meta = MetaData.builder()
                            .timestamp(LocalDateTime.now())
                            .build();
        // 로그인 리스트 획득
        List<LoginProviderDto> providers = oauth2Service.getLoginProviderList();
        log.info("로그인 목록 준비 완료");
        logPaint.sep("로그인 방식 목록 호출 이탈");
        return ResponseEntity.ok(ApiResponse.success(providers, meta));
    }

    // 2. 카카오 로그인 리다이렉트 url 생성
    @GetMapping("/oauth/login/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 로그인을 위해 Oauth server로 리다이렉트 합니다")
    public void redirectToKakao(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session) throws IOException {
        final String mode       = request.getHeader("Sec-Fetch-Mode");   // navigate / cors ...
        final String secPurpose = request.getHeader("Sec-Purpose");       // e.g. "prefetch;prerender" (Chrome)
        final String purpose    = request.getHeader("Purpose");           // e.g. "prefetch" (일부 UA)
        final String fetchUser  = request.getHeader("Sec-Fetch-User");    // "?1" if user activation (click)
        final String referer    = request.getHeader("Referer");
        final boolean fromSwagger = referer != null && referer.contains("/swagger-ui");

        logPaint.sep("redirectToKakao handler 진입");
        // 1) 내비게이션이 아닌 요청 차단 (XHR 등)
        if (!fromSwagger && mode != null && !"navigate".equalsIgnoreCase(mode)) {
            log.info("로그인 진입 차단: non-navigate 요청 (mode={}, secPurpose={}, purpose={}, user={})",
                    mode, secPurpose, purpose, fetchUser);
            logPaint.sep("redirectToKakao handler 이탈");
            response.sendError(400, "Login redirect must be a browser navigation.");
            return;
        }

        // 2) 프리페치/프리렌더 차단
        if ((secPurpose != null && (secPurpose.contains("prefetch") || secPurpose.contains("prerender")))
                || (purpose != null && purpose.contains("prefetch"))) {
            log.info("로그인 진입 차단: prefetch/prerender 감지 (mode={}, secPurpose={}, purpose={}, user={})",
                    mode, secPurpose, purpose, fetchUser);
            response.setHeader("Cache-Control", "no-store");
            response.setStatus(HttpServletResponse.SC_NO_CONTENT); // 204
            return;
        }

        // 4) 여기까지 통과한 경우에만 기존 로깅 + 리다이렉트
        // CSRF 방지용 state (세션 저장)
        String state = UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);
        log.info("session에 state 저장 완료 : {}", state);

        // kakao redirect url 생성 (scope 생략, stemp=login도 생략)
        String authorizeUrl = oauth2Service.getAuthorizeUrl(OAuthProvider.KAKAO, state);
        log.info("카카오 로그인 리다이렉트 생성 : {}", authorizeUrl);
        logPaint.sep("redirectToKakao handler 이탈");

        response.setHeader("Cache-Control", "no-store");
        response.sendRedirect(authorizeUrl);
    }

    // 3. 카카오 콜백 처리 - 사용자 정보 session 저장
    // URL ex) GET /oauth/kakao/callback?code=AUTH_CODE&state=xxx (state는 카카오 로그인 리다이렉트 url에 param으로 붙인 값)
    @Operation(
            summary = "Oauth server callback 처리 (서버 <-> Oauth server 전용)",
            description = "사용자가 Kakao 인증을 끝마치면 보안 코드를 통해 사용자 정보에 접근하여 받고 session에 저장한 뒤 sessionId를 내려줍니다."
    )
    @GetMapping("/oauth/kakao/callback")
    public ResponseEntity<ApiResponse<CallBackResponseDto>> kakaoCallback(
            @Valid @ModelAttribute KakaoCallbackRequestDto kakaoCallbackRequestDto,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {
        logPaint.sep("kakaoCallback 진입");

        String code = kakaoCallbackRequestDto.code();
        String returnedState = kakaoCallbackRequestDto.state();

        // 유효성 검사 - code 없음 (예외 가능성)
        if (code == null || code.isBlank()) {
            throw new KakaoOauthException(KakaoOauthErrorCode.C001);
        }

        // session에 저장된 state 값 꺼내서 CSRF 보안 검사 (예외 가능성)
        String sessionState = (String) session.getAttribute("oauth_state");
        log.info("callback url state parameter : {}", returnedState);
        log.info("client BE session 저장 state : {}", sessionState);
        if (sessionState == null || !sessionState.equals(returnedState)) {
            log.warn("❌ CSRF 의심: 세션의 state와 리턴된 state가 다릅니다.");
            // 실패 resopnse 만들어 반환하기
            throw new KakaoOauthException(KakaoOauthErrorCode.C002);
        }

        // state는 더 이상 쓸모없으니 세션에서 제거 (예외 가능성)
        session.removeAttribute("oauth_state");

        // 1. Access Token 요청
        // [INFO] Kakao Authorization Server로부터 access token을 교환
        String accessToken = oauth2Service.getAccessToken(code, OAuthProvider.KAKAO);

        // 2. 사용자 정보 획득
        // (해당 계층에서 Resource server와 통신, response body 값을 UserInfoParser를 사용해 userInfoDto로 가공하여 반환)
        OAuth2KakaoUserInfoDto userInfo = oauth2Service.getUserInfo(accessToken, OAuthProvider.KAKAO);

        JoinOrLoginResult result = memberJoinService.ensureOAuthMember(
                OAuthProvider.KAKAO,
                userInfo.getId(),
                userInfo.getName(),
                userInfo.getEmail()
        );

        Member member = result.member();
        boolean isNew = result.isNew();

        // 3. 사용자 정보 세션에 저장
        session.setAttribute("user", userInfo);
        log.info("[새로 가입한 회원 : {}] / [이름 {}] / [sessionId : {}]", isNew, member.getName(), session.getId());
        logPaint.sep("kakaoCallback 이탈");

        // 4. 클라이언트 홈으로 리다이렉트 (지금은 FE가 없으니 단순 return으로 테스트 실행)
        // response.sendRedirect("http://localhost:3000");

        // [INFO] 응답 메타에 한국시간 ISO-8601 타임스탬프 포함
        String timestamp = java.time.OffsetDateTime
                .now(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // 공통 응답 생성을 위한 meta 생성
        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();

        // 공통 응답 생성을 위한 data 생성
        CallBackResponseDto callBackResponseDto = CallBackResponseDto.builder()
                .sessionId(session.getId())
                // 내부 static class는 독립적으로 추가 build (이걸 위해서 inner class에도 @builder 사용)
                .user(
                    CallBackResponseDto.User.builder()
                        .id(String.valueOf(userInfo.getId()))
                        .name(userInfo.getName())
                        .email(userInfo.getEmail())
                        .build()
                )
                .build();

        return ResponseEntity.ok(ApiResponse.success(callBackResponseDto, meta));
    }

    // 로그아웃은 꽤나 중요한 서버 데이터 변경 처리이기에 body에 실을 데이터가 없다고 해도 Get보단 Post 방식으로 처리하는 것이 적절하다
    @PostMapping("/oauth/logout")
    @Operation(summary = "로그아웃", description = " 로그아웃을 지원합니다.")
    public ApiResponse<?> logout(@Valid @RequestBody LogoutRequestDto logoutDto, HttpSession session) {
        logPaint.sep("logOut handler 진입");

        LoginType type = logoutDto.getType();
        Provider provider = logoutDto.getProvider();
        log.info("logout request - type: {}, provider: {}", type, provider);

        session.invalidate();
        logPaint.sep("logOut handler 이탈");
        return ApiResponse.success(
                Map.of("message", "로그아웃 완료"),
                MetaData.builder()
                    .timestamp(LocalDateTime.now())
                    .build());
    }
}
