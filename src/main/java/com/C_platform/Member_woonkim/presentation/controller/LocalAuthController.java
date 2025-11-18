package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.LocalAuthUseCase;
import com.C_platform.Member_woonkim.application.useCase.EmailVerificationUseCase;
import com.C_platform.Member_woonkim.application.useCase.PasswordRecoveryUseCase;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LoginType;
import com.C_platform.Member_woonkim.exception.LocalAuthErrorCode;
import com.C_platform.Member_woonkim.exception.LocalAuthException;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.LoginRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.PasswordFindRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.PasswordResetRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.RandomCodeVerificationDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SendRandomCodeToEmailDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SignupRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.LoginResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SignupResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SuccessMessageResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Oauth.response.LoginCheckDto;
import com.C_platform.Member_woonkim.utils.CreateMetaData;
import com.C_platform.Member_woonkim.utils.LogPaint;
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

@RestController
@RequestMapping("/v1/local")
@RequiredArgsConstructor
@Slf4j
public class LocalAuthController {

    private final LocalAuthUseCase localAuthUseCase;
    private final EmailVerificationUseCase emailVerificationUseCase;
    private final PasswordRecoveryUseCase passwordRecoveryUseCase;


    @PostMapping("/email/random_code")
    @Operation(summary = "이메일 인증 코드 전송", description = "지정된 이메일로 6자리 인증 코드를 전송합니다.")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> sendRandomCodeToEmail(
            @Valid @RequestBody SendRandomCodeToEmailDto sendRandomCodeToEmailDto
    ) {
        LogPaint.sep("random code 생성 호출 진입");
        log.info("LocalAuthController.sendRandomCodeToEmail: 인증 코드 전송 요청 - email: {}", sendRandomCodeToEmailDto.email());

        SuccessMessageResponseDto response = emailVerificationUseCase.sendVerificationCode(sendRandomCodeToEmailDto);

        log.info("LocalAuthController.sendRandomCodeToEmail: 인증 코드 전송 성공 - email: {}", sendRandomCodeToEmailDto.email());

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), null);

        LogPaint.sep("random code 생성 호출 이탈");

        return ResponseEntity.ok(ApiResponse.success(response, meta));
    }

    @PostMapping("/email/verification")
    @Operation(summary = "이메일 인증 코드 검증", description = "입력된 인증 코드를 검증하고 이메일을 확인합니다.")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> randomCodeVerification(
            @Valid @RequestBody RandomCodeVerificationDto randomCodeVerificationDto
    ) {
        LogPaint.sep("이메인 인증 코드 검증 호출 진입");

        log.info("LocalAuthController.randomCodeVerification: 인증 코드 검증 요청 - email: {}", randomCodeVerificationDto.email());

        SuccessMessageResponseDto response = emailVerificationUseCase.verifyCode(randomCodeVerificationDto);

        log.info("LocalAuthController.randomCodeVerification: 인증 코드 검증 성공 - email: {}", randomCodeVerificationDto.email());

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), null);

        LogPaint.sep("이메인 인증 코드 검증 호출 이탈");

        return ResponseEntity.ok(ApiResponse.success(response, meta));
    }

    /**
     * 비밀번호 찾기 - 인증 코드 전송
     *
     * 사용자가 입력한 이메일로 비밀번호 재설정 인증 코드를 전송합니다.
     *
     * @param request 비밀번호 찾기 요청 (이메일)
     * @return 성공 메시지
     * @status 200 OK - 인증 코드 전송 성공
     * @status 400 Bad Request - 유효하지 않은 이메일 또는 가입되지 않은 이메일
     * @status 500 Internal Server Error - 이메일 전송 실패
     */
    @PostMapping("/password/find")
    @Operation(summary = "비밀번호 찾기 - 인증 코드 전송",
               description = "입력한 이메일로 비밀번호 재설정 인증 코드를 전송합니다")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> sendPasswordResetCode(
            @Valid @RequestBody PasswordFindRequestDto request
    ) {
        LogPaint.sep("sendPasswordResetCode yㅇ을 handler 진입");
        log.info("LocalAuthController.sendPasswordResetCode: 비밀번호 찾기 - 인증 코드 전송 요청 - email: {}", request.email());

        SuccessMessageResponseDto response = passwordRecoveryUseCase.sendPasswordResetCode(request);

        log.info("LocalAuthController.sendPasswordResetCode: 비밀번호 찾기 - 인증 코드 전송 성공 - email: {}", request.email());

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), null);

        LogPaint.sep("sendPasswordResetCode handler 이탈");

        return ResponseEntity.ok(ApiResponse.success(response, meta));
    }

    /**
     * 비밀번호 찾기 - 인증 코드 검증 및 비밀번호 재설정
     *
     * 사용자가 받은 인증 코드와 새로운 비밀번호를 입력하여 비밀번호를 재설정합니다.
     *
     * @param request 비밀번호 재설정 요청 (이메일, 인증 코드, 새 비밀번호)
     * @return 성공 메시지
     * @status 200 OK - 비밀번호 재설정 성공
     * @status 400 Bad Request - 유효하지 않은 입력값, 인증 코드 만료 또는 불일치, 가입되지 않은 이메일
     */
    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 찾기 - 비밀번호 재설정",
               description = "인증 코드를 검증하고 새로운 비밀번호로 변경합니다")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> resetPassword(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        LogPaint.sep("resetPassword handler 진입");
        log.info("LocalAuthController.resetPassword: 비밀번호 재설정 요청 - email: {}", request.email());

        SuccessMessageResponseDto response = passwordRecoveryUseCase.resetPassword(request);

        log.info("LocalAuthController.resetPassword: 비밀번호 재설정 성공 - email: {}", request.email());

        MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), null);

        LogPaint.sep("resetPassword handler 이탈");

        return ResponseEntity.ok(ApiResponse.success(response, meta));
    }

    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "회원가입을 합니다.")
    public ResponseEntity<ApiResponse<SignupResponseDto>> signup(
            @Valid @RequestBody SignupRequestDto request
    ) {
        LogPaint.sep("signup handler 진입");
        log.info("LocalAuthController.signup: 회원가입 요청 - email: {}", request.getEmail());

        SignupResponseDto response = localAuthUseCase.signup(request);

        log.info("LocalAuthController.signup: 회원가입 성공 - memberId: {}", response.getMemberId());

        MetaData metaData = MetaData.builder()
                .timestamp(java.time.LocalDateTime.now())
                .build();

        LogPaint.sep("signup handler 이탈");
        return ResponseEntity.ok(ApiResponse.success(response, metaData));
    }

    /**
     * 로그인 엔드포인트
     * <p>
     * 이 엔드포인트는 직접 처리되지 않으며,
     * JsonUsernamePasswordAuthenticationFilter와 LocalAuthenticationSuccessHandler에서 처리됩니다.
     *
     * @param request 로그인 요청 데이터 (email, password)
     * @return 로그인된 회원 정보
     * @status 200 OK - 로그인 성공
     * @status 401 Unauthorized - 로그인 실패 (잘못된 이메일/비밀번호)
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "로그인을 합니다.")
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
     * <p>
     * 이 엔드포인트는 직접 처리되지 않으며,
     * LogoutFilter와 LocalLogoutSuccessHandler에서 처리됩니다.
     *
     * @return 로그아웃 성공 응답
     * @status 200 OK - 로그아웃 성공
     */
    @PostMapping("/logout")
    @Operation(summary = "로그아웃", description = "로그아웃을 합니다.")
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
     * <p>
     * 현재 사용자가 Local 로그인 상태인지를 확인하고, 로그인한 경우 회원 정보를 반환합니다.
     *
     * @param xRequestId 요청 ID (선택)
     * @param request    HTTP 요청 객체
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
        LogPaint.sep("localLoginCheck handler 진입");
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

        LogPaint.sep("localLoginCheck handler 이탈");

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .body(ApiResponse.success(dto, meta));
    }
}
