package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.LocalAuthUseCase;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.exception.LocalAuthErrorCode;
import com.C_platform.Member_woonkim.exception.LocalAuthException;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.LoginRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SignupRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.LoginResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SignupResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LoginCheckDto;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Local 인증 컨트롤러
 * 회원가입, 로그인, 로그아웃을 처리합니다.
 * - 회원가입: Controller에서 직접 처리
 * - 로그인: JsonUsernamePasswordAuthenticationFilter + SuccessHandler에서 처리
 * - 로그아웃: LogoutFilter + LogoutSuccessHandler에서 처리
 */
@RestController
@RequestMapping("/v1/local")
@RequiredArgsConstructor
@Slf4j
public class LocalAuthController {

    private final LocalAuthUseCase localAuthUseCase;

    /**
     * 회원가입 엔드포인트
     *
     * @param request 회원가입 요청 데이터
     * @return 가입된 회원 정보
     * @status 201 Created
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request
    ) {
        log.info("LocalAuthController.signup: 회원가입 요청 - email: {}", request.getEmail());

        try {
            SignupResponseDto response = localAuthUseCase.signup(request);

            log.info("LocalAuthController.signup: 회원가입 성공 - memberId: {}", response.getMemberId());

            com.C_platform.global.MetaData metaData = com.C_platform.global.MetaData.builder()
                    .timestamp(java.time.LocalDateTime.now())
                    .build();

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(ApiResponse.success(response, metaData));

        } catch (IllegalArgumentException e) {
            log.warn("LocalAuthController.signup: 입력 값 검증 실패 - {}", e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("LocalAuthController.signup: 예상치 못한 오류", e);
            throw e;
        }
    }

    /**
     * 로그인 엔드포인트
     *
     * 이 엔드포인트는 직접 처리되지 않으며,
     * JsonUsernamePasswordAuthenticationFilter와 LocalAuthenticationSuccessHandler에서 처리됩니다.
     *
     * @param request 로그인 요청 데이터 (email, password)
     * @return 로그인된 회원 정보
     * @status 200 OK - 로그인 성공
     * @status 401 Unauthorized - 로그인 실패 (잘못된 이메일/비밀번호)
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponseDto>> login(
            @Valid @RequestBody LoginRequestDto request
    ) {
        // JsonUsernamePasswordAuthenticationFilter + LocalAuthenticationSuccessHandler가 처리
        // 이 메서드는 요청 검증 목적으로만 호출됩니다.
        // 실제 인증은 필터 체인에서 수행되고, SuccessHandler가 응답을 반환합니다.

        log.debug("LocalAuthController.login: 요청 검증 - email: {}", request.getEmail());

        // 이 메서드는 실제로 실행되지 않습니다.
        // 필터가 요청을 가로채서 인증 처리를 수행합니다.
        throw new IllegalStateException("이 메서드는 실행되지 않아야 합니다");
    }

    /**
     * 로그아웃 엔드포인트
     *
     * 이 엔드포인트는 직접 처리되지 않으며,
     * LogoutFilter와 LocalLogoutSuccessHandler에서 처리됩니다.
     *
     * @return 로그아웃 성공 응답
     * @status 200 OK - 로그아웃 성공
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        // LogoutFilter + LocalLogoutSuccessHandler가 처리
        // 이 메서드는 요청 매핑 목적으로만 존재합니다.
        // 실제 로그아웃 처리는 필터 체인에서 수행되고, LogoutSuccessHandler가 응답을 반환합니다.

        log.debug("LocalAuthController.logout: 로그아웃 요청");

        // 이 메서드는 실제로 실행되지 않습니다.
        // LogoutFilter가 요청을 가로채서 로그아웃 처리를 수행합니다.
        throw new IllegalStateException("이 메서드는 실행되지 않아야 합니다");
    }

    /**
     * Local 로그인 상태 확인 엔드포인트
     *
     * 현재 사용자가 Local 로그인 상태인지를 확인하고, 로그인한 경우 회원 정보를 반환합니다.
     *
     * @param xRequestId 요청 ID (선택)
     * @param request HTTP 요청 객체
     * @return 회원 정보 또는 에러 응답
     * @status 200 OK - 로그인 상태 확인 성공
     * @status 401 Unauthorized - 로그인하지 않은 상태
     */
    @GetMapping("/check")
    @Operation(summary = "로컬 로그인 확인", description = """
            현재 사용자가 로컬 로그인 상태인지를 확인해 줍니다.<br/>
            성공 시 : 사용자 정보 반환<br/>
            실패 시 : 실패 message 반환<br/>
            """)
    public ResponseEntity<ApiResponse<LoginCheckDto>> localLoginCheck(
            @Parameter(example = "req-129")
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            HttpServletRequest request
    ) {
        log.info("LocalAuthController.localLoginCheck: 로컬 로그인 상태 확인 요청");

        // 1. 세션 ID 로그 (디버깅 목적)
        String sessionId = request.getSession(false) == null ? "(없음)" : request.getSession(false).getId();
        log.debug("LocalAuthController.localLoginCheck: SessionId: {}", sessionId);

        // 2. SecurityContext에서 Authentication 추출
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // 3. 로그인 상태 검증
        if (auth == null || !auth.isAuthenticated()) {
            log.warn("LocalAuthController.localLoginCheck: Authentication이 null 또는 미인증 상태");
            throw new LocalAuthException(LocalAuthErrorCode.C003);
        }

        Object principal = auth.getPrincipal();
        if (principal == null || "anonymousUser".equals(principal.toString())) {
            log.warn("LocalAuthController.localLoginCheck: 익명 사용자 또는 인증되지 않은 상태");
            throw new LocalAuthException(LocalAuthErrorCode.C003);
        }

        // 4. email 추출 (Local 로그인에서 principal은 이메일 문자열)
        String email = auth.getName();
        log.info("LocalAuthController.localLoginCheck: 이메일 추출 - {}", email);

        // 5. DB에서 최신 Member 정보 조회
        Member member = localAuthUseCase.getMemberByEmail(email);

        // 6. LoginCheckDto 생성
        LoginCheckDto dto = LoginCheckDto.builder()
                .memberId(member.getMemberId())
                .memberName(member.getName())
                .memberNickname(member.getNickname())
                .loginType(LoginType.LOCAL.getLoginType())
                .provider(member.getLocalProvider().getProviderName()) // "LOCAL"
                .email(member.getEmail())
                .profileImageUrl(member.getProfileImageUrl())
                .build();

        log.info("LocalAuthController.localLoginCheck: 로컬 로그인 상태 확인 성공 - memberId: {}, email: {}",
                member.getMemberId(), email);

        // 7. 응답 생성
        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.success(dto, meta));
    }
}
