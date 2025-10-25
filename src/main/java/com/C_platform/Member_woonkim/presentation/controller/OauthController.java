package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.OAuth2UseCase;
import com.C_platform.Member_woonkim.domain.dto.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.entitys.CustomOAuth2User;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import com.C_platform.Member_woonkim.exception.OauthErrorCode;
import com.C_platform.Member_woonkim.exception.OauthException;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.request.CallbackRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.request.LogoutRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.*;
import com.C_platform.Member_woonkim.presentation.dtoAssembler.OauthAssembler;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
import com.C_platform.Member_woonkim.utils.InMemoryOauthSateStore;
import com.C_platform.Member_woonkim.utils.LogPaint;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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


    @Value("${app.identifier}")
    private String envIdentifier;

    @Value("${app.front-callback-path}")
    private String FRONT_CALLBACK_PATH;

    @Value("${app.base-url}")
    private String base_base;

    private static final String LOGOUT_SUCCESS = "로그아웃 성공";

    private final OAuth2UseCase oauth2UseCase; // 주요 로직들 처리

    private final OauthAssembler oauthAssembler; // 응답 dto 생성

    private final InMemoryOauthSateStore inMemoryOauthSateStore; // state를 저장하기 위한 인메모리 저장소

    // Kakao/Naver 로그인 공급자 목록 반환
    @GetMapping("/oauth/login")
    @Operation(summary = "로그인 방식 (Oauths, local) 목록 출력", description = " 서비스가 지원하는 로그인 방식을 조회 합니다.")
    public ResponseEntity<ApiResponse<List<LoginProviderResponseDto>>> getLoginProviders(
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {

        LogPaint.sep("로그인 방식 목록 호출 진입");
        log.info("[디버깅 목적] X-Request-Id : {}", xRequestId); // 값이 있는지 테스트

        // meta 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);

        // 로그인 리스트 획득
        List<LoginProviderResponseDto> providers = oauth2UseCase.loginProviderList();
        log.info("로그인 목록 준비 완료");
        LogPaint.sep("로그인 방식 목록 호출 이탈");
        return ResponseEntity.ok(ApiResponse.success(providers, meta));
    }

    // 2. oauth 리다이렉트 url 생성
    // response.redirect()에서 IOException check error가 발생할 수 있기에 throws 선언 필수
    @GetMapping("/oauth/login/{provider}")
    @Operation(summary = "Oauth 로그인", description = "Oauth 로그인을 위한 Oauth server url을 생성하여 내려줍니다")
    public ResponseEntity<ApiResponse<CreateRedirectUriResponseDto>> createRedirectUri(
            @PathVariable String provider,
            HttpServletRequest req,
            @Parameter(hidden = true) // swwagger ui에 표시 안 함
            @RequestHeader(value = "Referer", required = false) String referer,
            @Parameter(hidden = true) // swwagger ui에 표시 안 함
            @RequestHeader(value = "Origin", required = false) String originHeader,
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId
    ) {
        LogPaint.sep("createRedirectUri handler 진입");

        log.info("[디버깅 목적] origin : {}", originHeader); // 값이 있는지 테스트
        log.info("[디버깅 목적] X-Request-Id : {}", xRequestId); // 값이 있는지 테스트
        log.info("[디버깅 목적] referer : {}", referer); // 값이 있는지 테스트
        log.info("[디버깅 목적] provider : {}", provider); // 값이 있는지 테스트


        // TODO : 보안 검증 로직 Filter class로 빼기
        // 0) 프리페치/프리렌더 차단
        if (isPrefetchOrPrerender(req)) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build(); // 204
        }

        // 1) 요청 origin 저장 (callback 처리 시점에 oauth_state 검증 후 사용)
        String origin = extractOriginFromReferer(referer, originHeader);
        log.info("[디버깅 목적] origin {}", origin); // 값이 있는지 테스트

        // 1) CSRF 핵심 방어: state 생성/세션 저장
        // TODO : oauth , origin 값 session 저장 -> InMemory 저장 구조로 바꿔서 서브 파티션에서 쿠키 미적재 문제 우회
        OAuthProvider oauthProvider = getOauthProvider(provider);
        String stateCode = generateAndStoreState(inMemoryOauthSateStore, origin, oauthProvider);
        log.info("[디버깅 목적] InMemory에 저장한 stateCode 값 : {}", stateCode);

        // 2) 리다이렉트 주소 생성 ; oauthProvider 값에 맞춰서 uri 생성
        String authorizeUrl = oauth2UseCase.AuthorizeUrl(oauthProvider, stateCode);
        log.info("Oauth 로그인 리다이렉트 생성 : {}", authorizeUrl);

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);

        // TODO : 생성자 주입 -> assember도 생성자 주입이 아니라 register로 런타임에 찾도록 변경
        CreateRedirectUriResponseDto createRedirectUriResponseDto =
                oauthAssembler.getCreateRedirectUriResponseDto(authorizeUrl);

        log.info("[디버깅 목적] 현재 Env : {}", envIdentifier);
        LogPaint.sep("createRedirectUri handler 이탈");

        // 4) 어떤 요청이든 항상 JSON 반환 (리다이렉트 절대 안 함)
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store") // 해당 값 캐시에 남기지 않는다
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(createRedirectUriResponseDto, meta));
    }

    // TODO : 작성하기
    @Operation(
            summary = "Oauth server callback 처리 (서버 <-> Oauth server 전용)",
            description = "사용자가 Oauth 인증을 끝마치면 보안 코드를 통해 사용자 정보에 접근하여 받고 session에 저장한 뒤 sessionId를 내려줍니다."
    )
    @GetMapping("/oauth/{provider}/callback")
    public ResponseEntity<ApiResponse<CallBackResponseDto>> Callback(
            @PathVariable String provider,
            @Valid @ModelAttribute CallbackRequestDto callbackRequestDto,
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletRequest request,
            HttpServletResponse response, // response 파라미터 추가
            HttpSession session
    ) throws IOException {
        LogPaint.sep("Callback handler 진입 (고정 쿠키 테스트)");

        String origin = inMemoryOauthSateStore.consumeOrigin(callbackRequestDto.state(), getOauthProvider(provider));
        checkStateValidation(origin);

        // --- 실제 로그인 로직은 테스트를 위해 잠시 비활성화 ---
        // OAuth2UserInfoDto userInfo = oauth2UseCase.getUserInfo(callbackRequestDto.code(), callbackRequestDto.state(), getOauthProvider(provider));
        // JoinOrLoginResult result = oauth2UseCase.ensureOAuthMember(userInfo, getOauthProvider(provider));
        // Member member = result.member();
        // establishSecurityContext(member, session);
        // session.setAttribute("user", userInfo);

        // 고정된 테스트 쿠키 생성
        ResponseCookie testCookie = ResponseCookie.from("test-cookie", "test-value-123")
                .httpOnly(true)
                .secure(true)
                .sameSite("None")
                .path("/")
                .maxAge(60 * 15) // 15분
                .build();

        log.info("[고정 쿠키 테스트] Set-Cookie 헤더에 테스트 쿠키를 설정합니다. 값: {}", testCookie.toString());

        String redirectUrl = origin + FRONT_CALLBACK_PATH;

        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, redirectUrl)
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.SET_COOKIE, testCookie.toString())
                .body(null);
    }

    // 로그아웃은 꽤나 중요한 서버 데이터 변경 처리이기에 body에 실을 데이터가 없다고 해도 Get보단 Post 방식으로 처리하는 것이 적절하다
    @PostMapping("/oauth/logout")
    @Operation(summary = "로그아웃", description = " 로그아웃을 지원합니다.")
    public ResponseEntity<ApiResponse<LogoutResponseDto>> logout(
            @Valid @RequestBody LogoutRequestDto logoutDto,
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpSession session
    ) {
        LogPaint.sep("logOut handler 진입");

        LoginType type = logoutDto.getType();
        Provider provider = logoutDto.getProvider();
        log.info("logout request - type: {}, provider: {}", type, provider);
        log.info("[디버깅 목적] X-Request-Id : {}", xRequestId); // 값이 있는지 테스트

        try {
            session.invalidate(); // server login session 삭제
            log.info("로그인 세션 삭제 완료");
        } catch (IllegalStateException ignored) {
            throw new OauthException(OauthErrorCode.C011);
        }

        final String expiredSessionCookie = buildExpiredSessionCookieForEnv(isProd()); // brwoser JSESSIONID 쿠키 삭제
        log.info("browser cookie Max-age = 0으로 설정 (자연 삭제 목적)");

        // 응답 data 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);

        LogoutResponseDto logoutResponseDto
                = oauthAssembler.getLogoutResponseDto(LOGOUT_SUCCESS);

        LogPaint.sep("logOut handler 이탈");
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredSessionCookie) // Max-age = 0 cookie를 통해 browser 쿠키 삭제
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(logoutResponseDto, meta));
    }

    @GetMapping("/oauth/login/check")
    @Operation(summary = "로그인 확인", description = """
            현재 사용자가 로그인 상태인지를 확인해 줍니다 <br/>
            성공 시 : 사용자 정보 반환 <br/>
            실패 시 : 실패 message 반환 <br/>
            """)
    public ResponseEntity<ApiResponse<String>> loginCheck(
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletRequest request // 쿠키 확인을 위해 파라미터 추가
    ) {
        LogPaint.sep("loginCheck 진입 (고정 쿠키 테스트)");

        String cookies = request.getHeader("Cookie");
        log.info("[고정 쿠키 테스트] /login/check로 들어온 쿠키 헤더: {}", cookies != null ? cookies : "(쿠키 없음)");

        // 테스트를 위해 임시로 성공 응답 반환
        return ResponseEntity.ok(ApiResponse.success("Cookie check complete. See logs.", null));
    }

// ----------------------------- Helper (instance) ---------------------------------------

    private boolean isProd() {
        return "prod".equalsIgnoreCase(envIdentifier);
    }

    // origin이 null이 아니면 그대로 반환, origin이 없다면 referer parsing, referer도 없다면 null 반환
    private String extractOriginFromReferer(String referer, String originHeader) {
        if (originHeader != null) {
            return originHeader;
        } else if (referer == null || referer.isBlank()) {
            return null;
        }
        try {
            URI uri = new URI(referer);
            String scheme = uri.getScheme();  // http, https
            String host = uri.getHost();      // localhost, example.com
            int port = uri.getPort();         // -1이면 기본 포트

            String origin = scheme + "://" + host;
            if (port != -1 && port != 80 && port != 443) {
                origin += ":" + port;
            }
            return origin;
        } catch (URISyntaxException e) {
            return null;
        }
    }

// ----------------------------- Helper (static) ---------------------------------------

    // redirectToKakao handler에서 사용
    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    // pathVariable로 들어온 provider에 따라 OauthProvider Enum을 반환하는 method
    private static OAuthProvider getOauthProvider(String provider) {
        return switch (provider) {
            case "kakao" -> OAuthProvider.KAKAO;
            case "naver" -> OAuthProvider.NAVER;
            default -> throw new OauthException(OauthErrorCode.C009);
        };
    }

    // 배포 환경에 따라 session 설정 분기
    private static String buildExpiredSessionCookieForEnv(boolean isProd) {
        final boolean secure   = isProd;
        final String  sameSite = isProd ? "None" : "Lax";

        return ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .maxAge(0)
                .build()
                .toString();
    }

    // kakaoCallback handler에서 사용
    private static void checkStateValidation(String origin) {
        if (origin == null) {
            log.warn("❌ CSRF 의심: 세션의 state와 리턴된 state가 다릅니다.");
            throw new OauthException(OauthErrorCode.C004);
        }
    }

    // prefetch 검사
    private static boolean isPrefetchOrPrerender(HttpServletRequest req) {
        String secPurpose = nvl(req.getHeader("Sec-Purpose")); // e.g. "prefetch;prerender"
        String purpose = nvl(req.getHeader("Purpose"));     // 일부 UA: "prefetch"
        return secPurpose.contains("prefetch")
                || secPurpose.contains("prerender")
                || purpose.contains("prefetch");
    }

    // (1) state 생성 후 세션 저장
    private static String generateAndStoreState(InMemoryOauthSateStore inMemoryOauthSateStore, String origin, OAuthProvider provider) {
        String oauth_state = UUID.randomUUID().toString();
        inMemoryOauthSateStore.put(oauth_state, origin, provider);
        return oauth_state;
    }

    // 로그인 정보를 확인하기 위해 customOAuth2User에 정보 등록
    private static void establishSecurityContext(Member member, HttpSession session) {
        List<SimpleGrantedAuthority> authorities =
                List.of(new SimpleGrantedAuthority("ROLE_USER"));

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("id", member.getMemberId());
        attributes.put("name", member.getName());
        attributes.put("email", member.getEmail());

        CustomOAuth2User principal = new CustomOAuth2User(
                member.getMemberId(),
                attributes,
                authorities
        );

        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
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
    }

    /**
     * sessionId cookie 생성 helper method
     * Cookie는 14일 유지
     * https가 아니므로 sucre = false
     **/
    private static void writeSessionCookie(HttpServletResponse response, HttpSession session) {
        writeSessionCookie(response, session, true, 1209600, "None"); // 14일
    }

    // writeSessionCookie에서 호출
    private static void writeSessionCookie(
            HttpServletResponse response,
            HttpSession session,
            boolean secure,
            long maxAgeSeconds,
            String sameSite // "Lax" | "Strict" | "None"
    ) {
        ResponseCookie cookie = ResponseCookie.from("JSESSIONID", session.getId())
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private static void expireSessionCookie(HttpServletResponse response) {
        ResponseCookie sessionClear = ResponseCookie.from("JSESSIONID", "")
                .path("/")
                .httpOnly(true)
                .secure(true)
                .sameSite("Lax")
                .maxAge(0) // 삭제 유도
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, sessionClear.toString());
    }

    private static void wirte_debug_log(HttpServletRequest request) {
        log.info("[CB] uri={}, query={}, dispatcherType={}, remoteAddr={}",
                request.getRequestURI(),
                request.getQueryString(),
                request.getDispatcherType(),     // REQUEST / FORWARD / INCLUDE / ASYNC
                request.getRemoteAddr());

        log.info("[CB] ua={}, referer={}, sec-fetch-site={}, sec-fetch-mode={}, sec-purpose={} \n",
                request.getHeader("User-Agent"),
                request.getHeader("Referer"),
                request.getHeader("Sec-Fetch-Site"),
                request.getHeader("Sec-Fetch-Mode"),
                request.getHeader("Sec-Purpose"));
    }
}

