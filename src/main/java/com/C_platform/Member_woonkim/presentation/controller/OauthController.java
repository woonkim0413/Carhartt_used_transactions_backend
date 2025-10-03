package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.service.MemberJoinService;
import com.C_platform.Member_woonkim.application.service.OAuth2Service;
import com.C_platform.Member_woonkim.domain.Oauth.CustomOAuth2User;
import com.C_platform.Member_woonkim.domain.Oauth.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import com.C_platform.Member_woonkim.exception.KakaoOauthErrorCode;
import com.C_platform.Member_woonkim.exception.KakaoOauthException;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.presentation.dto.CallBackResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.KakaoCallbackRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.LoginProviderDto;
import com.C_platform.Member_woonkim.presentation.dto.LogoutRequestDto;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
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
    // response.redirect()에서 IOException check error가 발생할 수 있기에 throws 선언 필수
    @GetMapping("/oauth/login/kakao")
    @Operation(summary = "카카오 로그인", description = "카카오 로그인을 위해 Oauth server로 리다이렉트 합니다")
    public ResponseEntity<?> redirectToKakao (
            HttpServletRequest req,
            HttpSession session)
    {
        logPaint.sep("redirectToKakao handler 진입");
        // 0) 프리페치/프리렌더 차단
        String secPurpose = nvl(req.getHeader("Sec-Purpose")); // e.g. "prefetch;prerender"
        String purpose = nvl(req.getHeader("Purpose"));     // 일부 UA: "prefetch"
        boolean isPrefetchOrPrerender =
                secPurpose.contains("prefetch") || secPurpose.contains("prerender") || purpose.contains("prefetch");
        if (isPrefetchOrPrerender) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build(); // 204
        }

        // 1) CSRF 핵심 방어: state 생성/세션 저장
        String state = java.util.UUID.randomUUID().toString();
        session.setAttribute("oauth_state", state);
        log.info("session에 state 저장 완료 : {}", state);

        String authorizeUrl = oauth2Service.getAuthorizeUrl(OAuthProvider.KAKAO, state);
        log.info("카카오 로그인 리다이렉트 생성 : {}", authorizeUrl);

        // 2) XHR/Swagger면 URL을 JSON으로 반환 → 사용자가 클릭해서 네비게이션 유도
        String mode = req.getHeader("Sec-Fetch-Mode"); // "navigate" | "cors" | "no-cors" ...
        boolean isXhr = mode != null && !"navigate".equalsIgnoreCase(mode);
        logPaint.sep("redirectToKakao handler 이탈");

        // 3) 어떤 요청이든 항상 JSON 반환 (리다이렉트 절대 안 함)
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(java.util.Map.of("authorizeUrl", authorizeUrl));
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
        String sessionState = (String) session.getAttribute("oauth_state"); // session에 저장된 state 값 꺼내서 비교 보안 검사 (예외 가능성)

        log.info("callback url state parameter : {}", returnedState);
        log.info("client BE session 저장 state : {}", sessionState);

        checkStateValidation(sessionState, returnedState); // req param과 session state 비교, 틀리면 예외

        session.removeAttribute("oauth_state"); // state는 더 이상 쓸모없으니 세션에서 제거 (예외 가능성)

        // 1. Access Token 요청
        // [INFO] Kakao Authorization Server로부터 access token을 교환
        String accessToken = oauth2Service.getAccessToken(code, OAuthProvider.KAKAO);

        // 2. 사용자 정보 획득
        // (해당 계층에서 Resource server와 통신, response body 값을 UserInfoParser를 사용해 userInfoDto로 가공하여 반환)
        OAuth2UserInfoDto userInfo = oauth2Service.getUserInfo(accessToken, OAuthProvider.KAKAO);

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

        // 5) todo : test : ✅ Spring Security 인증 세팅 (CustomOAuth2User 사용) (@AuthenticationPrincipal 사용 가능하도록)
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id",    member.getMemberId());
        attributes.put("name",  member.getName());
        attributes.put("email", member.getEmail());

        CustomOAuth2User principal = new CustomOAuth2User(
                member.getMemberId(),
                attributes,
                authorities
        );

        var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, authorities
        );

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        // 세션에도 SecurityContext 저장 (중요)
        session.setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        log.info("[새로 가입한 회원 : {}] / [이름 {}] / [닉네임 {}] [sessionId : {}]",
                isNew, member.getName(), member.getNickname(), session.getId());
        logPaint.sep("kakaoCallback 이탈");

        // 4. set-cookies header 추가하기 위한 객체 생성
        ResponseCookie sessionCookie = ResponseCookie.from("SESSION", session.getId())
                .httpOnly(true)
                .secure(false)  // test를 위해 꺼놓음
                .sameSite("Lax")
                .path("/")
                .maxAge(1209600)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, sessionCookie.toString());

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

        // todo : 해당 json 실어서 태규님이 주신 url로 리다이렉트 하는 방법 찾아보기
        // 클라이언트 홈으로 리다이렉트 (지금은 FE가 없으니 단순 return으로 테스트 실행)
        // response.sendRedirect("http://localhost:3000");
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

    // redirectToKakao handler에서 사용
    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    // kakaoCallback handler에서 사용
    private void checkStateValidation(String sessionState, String returnedState) {
        if (sessionState == null || !sessionState.equals(returnedState)) {
            log.warn("❌ CSRF 의심: 세션의 state와 리턴된 state가 다릅니다.");
            // 실패 resopnse 만들어 반환하기
            throw new KakaoOauthException(KakaoOauthErrorCode.C002);
        }
    }
}

