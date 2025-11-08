package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.LocalAuthUseCase;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.LoginRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SignupRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.LoginResponseDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SignupResponseDto;
import com.C_platform.global.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
