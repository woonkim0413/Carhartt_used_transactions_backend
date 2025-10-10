package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.OAuth2UseCase;
import com.C_platform.Member_woonkim.domain.dto.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.entitys.CustomOAuth2User;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.domain.enums.OAuthProvider;
import com.C_platform.Member_woonkim.domain.interfaces.Provider;
import com.C_platform.Member_woonkim.exception.KakaoOauthErrorCode;
import com.C_platform.Member_woonkim.exception.KakaoOauthException;
import com.C_platform.Member_woonkim.infrastructure.dto.OAuth2UserInfoDto;
import com.C_platform.Member_woonkim.presentation.Assembler.OauthAssembler;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.request.KakaoCallbackRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.request.LogoutRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.*;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
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
    private String identifier;

    @Value("${app.front-callback-path}")
    private String FRONT_CALLBACK_PATH;

    private static final String LOGOUT_SUCCESS = "로그아웃 성공";

    private final OAuth2UseCase oauth2UseCase; // 주요 로직들 처리

    private final OauthAssembler assembler; // 응답/요청 Dto 생성

    private final OauthAssembler oauthAssembler; // 응답 dto 생성

    // Kakao/Naver 로그인 공급자 목록 반환
    @GetMapping("/oauth/login")
    @Operation(summary = "로그인 방식 (Oauths, local) 목록 출력", description = " 서비스가 지원하는 로그인 방식을 조회 합니다.")
    public ResponseEntity<ApiResponse<List<LoginProviderResponseDto>>> getLoginProviders() {

        String requestId = UUID.randomUUID().toString();

        LogPaint.sep("로그인 방식 목록 호출 진입");
        // meta 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());

        // 로그인 리스트 획득
        List<LoginProviderResponseDto> providers = oauth2UseCase.loginProviderList();
        log.info("로그인 목록 준비 완료");
        LogPaint.sep("로그인 방식 목록 호출 이탈");
        return ResponseEntity.ok(ApiResponse.success(providers, meta));
    }

    // 2. 카카오 로그인 리다이렉트 url 생성
    // response.redirect()에서 IOException check error가 발생할 수 있기에 throws 선언 필수
    @GetMapping("/oauth/login/kakao")
    // todo : responseDto 생성 필요
    @Operation(summary = "카카오 로그인", description = "카카오 로그인을 위한 Oauth server url을 생성하여 내려줍니다")
    public ResponseEntity<ApiResponse<RedirectToKakaoResponseDto>> redirectToKakao(
            HttpServletRequest req,
            HttpSession session,
            @Parameter(hidden = true)
            @RequestHeader(value = "Referer", required = false) String referer
    ) {
        LogPaint.sep("redirectToKakao handler 진입");

        log.info("[디버깅 목적] referer {}", referer); // 값이 있는지 테스트
        log.info("[디버깅 목적] JsessionId {}", session.getId()); // 값이 있는지 테스트

        // TODO : 보안 검증 로직 Filter class로 빼기
        // 0) 프리페치/프리렌더 차단
        if (isPrefetchOrPrerender(req)) {
            return ResponseEntity.noContent()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .build(); // 204
        }

        // 1) CSRF 핵심 방어: state 생성/세션 저장
        String stateCode = generateAndStoreState(session, "oauth_state");

        // TODO : prod 환경일 때만 저장하도록 변경
        // 1) 요청 origin 저장 (callback 처리 시점에 oauth_state 검증 후 사용)
        String origin = extractOriginFromReferer(referer);
        session.setAttribute("origin", origin);
        log.info("[디버깅 목적] origin {}", origin); // 값이 있는지 테스트

        // 2) 리다이렉트 주소 생성
        String authorizeUrl = oauth2UseCase.AuthorizeUrl(OAuthProvider.KAKAO, stateCode);
        log.info("카카오 로그인 리다이렉트 생성 : {}", authorizeUrl);

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());

        RedirectToKakaoResponseDto redirectToKakaoResponseDto =
                assembler.getRedirectToKakaoResponseDto(authorizeUrl);

        LogPaint.sep("redirectToKakao handler 이탈");

        // 4) 어떤 요청이든 항상 JSON 반환 (리다이렉트 절대 안 함)
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store") // 해당 값 캐시에 남기지 않는다
                .header(HttpHeaders.PRAGMA, "no-cache")
                .body(ApiResponse.success(redirectToKakaoResponseDto, meta));
    }


    // TODO : (나중에) pathVariable을 보고 OauthProvider에 OAuthProvider.KAKAO를 주입할지 OAuthProvider.NAVER을 주입할지 결정
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
        LogPaint.sep("kakaoCallback 진입");

        String stateCode = kakaoCallbackRequestDto.code();
        String returnedState = kakaoCallbackRequestDto.state();
        String sessionState = (String) session.getAttribute("oauth_state"); // session에 저장된 state 값 꺼내서 비교 보안 검사 (예외 가능성)
        String origin = (String) session.getAttribute("origin"); // origin에 따른 분기 시에 사용
        session.removeAttribute("origin"); // origin 꺼낸 뒤에 session은 파괴

        log.info("callback url state parameter : {}", returnedState);
        log.info("client BE session 저장 state : {}", sessionState);

        // TODO : 예외 생성
        checkStateValidation(sessionState, returnedState); // req param과 session state 비교

        session.removeAttribute("oauth_state"); // state는 더 이상 쓸모없으니 세션에서 제거, session 이름은 redirect에서 만든 세션 이름과 동일해야 함

        OAuth2UserInfoDto userInfo = oauth2UseCase.getUserInfo(stateCode, OAuthProvider.KAKAO); // 사용자 정보 획득

        JoinOrLoginResult result = oauth2UseCase.ensureOAuthMember(userInfo, OAuthProvider.KAKAO); // 회원가입 유무에 따라 값 반환

        Member member = result.member();
        boolean isNew = result.isNew();

        // TODO : UseCase 내로 삽입하기
        // 4. 로그인 정보 @AuthenticationPrincipal로 가져올 수 있도록 처리
        establishSecurityContext(member, session);

        // 3. 사용자 정보 세션에 저장
        session.setAttribute("user", userInfo);

        log.info("[새로 가입한 회원 : {}] / [이름 {}] / [닉네임 {}] [sessionId : {}]",
                isNew, member.getName(), member.getNickname(), session.getId());

        // TODO : 세션 로직 따로 클래스 빼기
        // 5. set-cookies header 추가하기 위한 객체 생성
        writeSessionCookie(response, session);

        // 공통 응답 생성을 위한 meta 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());

        // 공통 응답 생성을 위한 data 생성
        CallBackResponseDto callBackResponseDto
                = oauthAssembler.getCallBackResponseDto(session, userInfo);

        LogPaint.sep("kakaoCallback 이탈");
        // EC2에서 요청을 보내는 경우 반환 (환경 파일에 적혀져 있는 값을 통해서 분기)
        if ("prod".equalsIgnoreCase(identifier)) {
            // 302 Redirect (쿠키는 이미 response에 set 되어 있으므로 그대로 전달됨)
            log.info("[(로그인 후) redirect origin] = {}", origin + FRONT_CALLBACK_PATH);
            return ResponseEntity.status(HttpStatus.FOUND)
                    .header(HttpHeaders.LOCATION, origin + FRONT_CALLBACK_PATH) // FRONT_ORIGIN은 pord 설정 파일에서 가져온 값
                    .header(HttpHeaders.CACHE_CONTROL, "no-store") // 민감 응답 캐싱 방지(선택)
                    .body(null); // 반환 타입을 유지하기 위해 null 본문
        }

        // local: callback url 그대로 반환
        return ResponseEntity.ok(ApiResponse.success(callBackResponseDto, meta));
    }

    // 로그아웃은 꽤나 중요한 서버 데이터 변경 처리이기에 body에 실을 데이터가 없다고 해도 Get보단 Post 방식으로 처리하는 것이 적절하다
    @PostMapping("/oauth/logout")
    @Operation(summary = "로그아웃", description = " 로그아웃을 지원합니다.")
    public ResponseEntity<ApiResponse<LogoutResponseDto>> logout(
            @Valid @RequestBody LogoutRequestDto logoutDto,
            HttpSession session
    ) {
        LogPaint.sep("logOut handler 진입");

        LoginType type = logoutDto.getType();
        Provider provider = logoutDto.getProvider();
        log.info("logout request - type: {}, provider: {}", type, provider);

        session.invalidate();

        // 응답 data 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());

        LogoutResponseDto logoutResponseDto
                = oauthAssembler.getLogoutResponseDto(LOGOUT_SUCCESS);

        LogPaint.sep("logOut handler 이탈");
        return ResponseEntity.ok(ApiResponse.success(logoutResponseDto, meta));
    }

    @GetMapping("/oauth/login/check")
    @Operation(summary = "로그인 확인", description = """
            현재 사용자가 로그인 상태인지를 확인해 줍니다 <br/>
            성공 시 : 사용자 정보 반환 <br/>
            실패 시 : 실패 message 반환 <br/>
            """)
    public ResponseEntity<ApiResponse<LoginCheckDto>> loginCheck(
            HttpSession session
    ) {
        LogPaint.sep("loginCheck 진입");

        // 1) 세션에서 로그인 정보 조회
        Object loginInfoBySession = session.getAttribute("user");

        // TODO : 실패 시 에러 코드 반환하도록 전역 에러 핸들러에서 코드 작성
        if (loginInfoBySession == null) {
            throw new KakaoOauthException(KakaoOauthErrorCode.C003);
        }

        if (!(loginInfoBySession instanceof OAuth2UserInfoDto)) {
            throw new KakaoOauthException(KakaoOauthErrorCode.C003);
        }

        // db에서 login session 정보를 바탕으로 member 조회
        Member member = oauth2UseCase.getMemberBySessionInfo((OAuth2UserInfoDto) loginInfoBySession);

        LoginCheckDto dto = LoginCheckDto.builder()
                .memberId(member.getMemberId())
                .memberName(member.getName())
                .memberNickname(member.getNickname())
                .loginType(LoginType.OAUTH)
                .provider(member.getOauthProvider()) // enum OAuthProvider
                .build();

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now());

        LogPaint.sep("loginCheck 이탈");

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.success(dto, meta));
    }

// ----------------------------- Helper (static) ---------------------------------------

    // redirectToKakao handler에서 사용
    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    // kakaoCallback handler에서 사용
    private static void checkStateValidation(String sessionState, String returnedState) {
        if (sessionState == null || !sessionState.equals(returnedState)) {
            log.warn("❌ CSRF 의심: 세션의 state와 리턴된 state가 다릅니다.");
            // 실패 resopnse 만들어 반환하기
            throw new KakaoOauthException(KakaoOauthErrorCode.C002);
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
    private static String generateAndStoreState(HttpSession session, String sessionName) {
        String state = UUID.randomUUID().toString();
        session.setAttribute(sessionName, state);
        log.info("session에 state 저장 완료 : {}", state);
        return state;
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
        writeSessionCookie(response, session, false, 1209600, "None"); // 14일
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

    private String extractOriginFromReferer(String referer) {
        if (referer == null || referer.isBlank()) return null;

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
}

