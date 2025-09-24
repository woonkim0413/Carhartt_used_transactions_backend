package com.C_platform.Member.ui.controller;

import com.C_platform.Member.domain.Oauth.OAuth2KakaoUserInfoDto;
import com.C_platform.Member.domain.Oauth.OAuth2Service;
import com.C_platform.Member.domain.Oauth.OAuthProvider;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.global.logPaint;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    // Kakao/Naver 로그인 공급자 목록 반환
    @GetMapping("/oauth/login")
    @Operation(summary = "로그인 방식 (Oauths, local) 목록 출력", description = " 서비스가 지원하는 로그인 방식을 조회 합니다.")
    public ApiResponse<Map<String, Object>> getLoginProviders() {
        String requestId = UUID.randomUUID().toString();

        logPaint.sep("로그인 방식 목록 호출 진입");
        List<Map<String, Object>> providers = List.of(
                Map.of(
                        "provider", "kakao",
                        "display_name", "카카오",
                        "type", "oauth",
                        "authorize_url", "http://43.203.218.247:8080//v1/auth/login/kakao"
                ),
                Map.of(
                        "provider", "naver",
                        "display_name", "네이버",
                        "type", "oauth",
                        "authorize_url", "http://43.203.218.247:8080/v1/auth/login/naver"
                )
        );
        log.info("로그인 목록 준비 완료");
        logPaint.sep("로그인 방식 목록 호출 이탈");
        return ApiResponse.success(
                Map.of("providers", providers),
                MetaData.builder().timestamp(LocalDateTime.now()).build()
        );
    }

    // 2. 카카오 로그인 리다이렉트 url 생성
    @GetMapping("/oauth/login/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 로그인을 위해 Oauth server로 리다이렉트 합니다")
    public void redirectToKakao(HttpServletRequest request, HttpServletResponse response, HttpSession session) throws IOException {
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

        // 3) (선택 권장) 사용자 제스처 없는 호출 차단
        //    실제 클릭이 아니면 막는다. 필요 없다면 이 블록은 주석 처리해도 됨.
//        if (!"?1".equals(fetchUser)) {
//            log.info("로그인 진입 차단: user activation 아님 (mode={}, secPurpose={}, purpose={}, user={})",
//                    mode, secPurpose, purpose, fetchUser);
//            response.sendError(400, "User activation required");
//            return;
//        }

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
    public Map<String, Object> kakaoCallback(
            @RequestParam("code") String code,
            @RequestParam("state") String returnedState,
            HttpSession session,
            HttpServletResponse response
    ) throws IOException {
        logPaint.sep("kakaoCallback 진입");

        // 유효성 검사 - code 없음
        // todo 실패하는 경우에 대한 response 코드 작성 필요
        if (code == null || code.isBlank()) {
            response.sendRedirect("/login?error=missing_code");
            return Map.of("success", false, "error", "missing_code");
        }

        // session에 저장된 state 값 꺼내서 CSRF 보안 검사
        String sessionState = (String) session.getAttribute("oauth_state");
        log.info("callback url state parameter : {}", returnedState);
        log.info("client BE session 저장 state : {}", sessionState);
        if (sessionState == null || !sessionState.equals(returnedState)) {
            log.warn("❌ CSRF 의심: 세션의 state와 리턴된 state가 다릅니다.");
            response.sendRedirect("/login?error=invalid_state"); // ❗보안 거부
            // 실패 resopnse 만들어 반환하기
            return Map.of("success", false, "error", "invalid_state");
        }

        // state는 더 이상 쓸모없으니 세션에서 제거
        session.removeAttribute("oauth_state");

        // 1. Access Token 요청
        // [INFO] Kakao Authorization Server로부터 access token을 교환
        String accessToken = oauth2Service.getAccessToken(code, OAuthProvider.KAKAO);

        // 2. 사용자 정보 획득
        // (해당 계층에서 Resource server와 통신, response body 값을 UserInfoParser를 사용해 userInfoDto로 가공하여 반환)
        OAuth2KakaoUserInfoDto userInfo = oauth2Service.getUserInfo(accessToken, OAuthProvider.KAKAO);

        // 3. 사용자 정보 세션에 저장
        session.setAttribute("user", userInfo);
        log.info("{} sessionId : {}", userInfo.getName(), session.getId());
        logPaint.sep("kakaoCallback 이탈");

        // 4. 클라이언트 홈으로 리다이렉트 (지금은 FE가 없으니 단순 return으로 테스트 실행)
        // response.sendRedirect("http://localhost:3000");

        // [INFO] 응답 메타에 한국시간 ISO-8601 타임스탬프 포함
        String timestamp = java.time.OffsetDateTime
                .now(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME);

        // [TODO] userInfo의 특정 필드가 null 가능하면 Map.of 대신 put 방식으로 안전 처리 고려
        return Map.of(
                "success", true,
                "data", Map.of(
                        "user", Map.of(
                                // Kakao Oauth Resource Server에서 user을 식별할 때 사용하는 key값
                                "id", userInfo.getId(),
                                "name", userInfo.getName(),
                                "email", userInfo.getEmail()
                        ),
                        // WAS에 저장된 특정 HttpSession을 식별하여 handler에 보내줄 때 사용
                        "sessionId", session.getId()
                ),
                "meta", Map.of(
                        "timestamp", timestamp
                )
        );
    }

    // 로그아웃은 꽤나 중요한 서버 데이터 변경 처리이기에 body에 실을 데이터가 없다고 해도 Get보단 Post 방식으로 처리하는 것이 적절하다
    // todo 로그아웃 요청 Json 받는 Dto 생성 필요 + 로컬 구현 및 naver Oauth를 구현한다고 하면 관련 처리 로직 생성 필요
    @PostMapping("/oauth/logout")
    @Operation(summary = "로그아웃", description = " 로그아웃을 지원합니다.")
    public ApiResponse<?> logout(@RequestBody Map<String, String> body, HttpSession session) {
        logPaint.sep("logOut handler 진입");

        String type = body.getOrDefault("type", "");
        String provider = body.getOrDefault("provider", "");
        log.info("logout request - type: {}, provider: {}", type, provider);

        session.invalidate();
        logPaint.sep("logOut handler 이탈");
        return ApiResponse.success(
                Map.of("message", "로그아웃 완료"),
                MetaData.builder().timestamp(LocalDateTime.now()).build());
    }
}
