package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.domain.dto.JoinOrLoginResult;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.domain.service.MemberJoinService;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SignupRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SignupResponseDto;
import com.C_platform.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Local 인증 UseCase
 *
 * 회원가입 등 Local 인증 관련 비즈니스 로직을 처리합니다.
 * 로그인/로그아웃은 Spring Security Filter에서 처리합니다.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class LocalAuthUseCase {

    private final MemberJoinService memberJoinService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Local 회원가입 처리
     *
     * 1. 요청 데이터 검증 (이미 Controller의 @Valid에서 처리됨)
     * 2. 비밀번호 암호화 (BCrypt)
     * 3. 이메일 중복 확인 및 회원 생성
     * 4. 응답 DTO 생성 및 반환
     *
     * @param request 회원가입 요청 DTO
     * @return 회원가입 응답 DTO
     * @throws IllegalArgumentException 입력 값이 유효하지 않을 때
     */
    public SignupResponseDto signup(SignupRequestDto request) {
        log.info("LocalAuthUseCase.signup: 회원가입 시작 - email: {}", request.getEmail());

        // 1. 입력 데이터 검증 (추가 검증 필요 시 여기서 처리)
        validateSignupRequest(request);

        // 2. 비밀번호 암호화 (BCrypt)
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        log.debug("LocalAuthUseCase.signup: 비밀번호 암호화 완료");

        // 3. 회원 생성 또는 기존 회원 반환
        // - 새 회원인 경우: 데이터베이스에 저장 및 신규 회원 반환
        // - 기존 회원인 경우: 이메일 중복 예외 발생 (MemberJoinService에서 처리)
        JoinOrLoginResult result = memberJoinService.ensureLocalMember(
                LocalProvider.LOCAL,
                request.getEmail(),
                encodedPassword,
                request.getName()
        );

        log.info("LocalAuthUseCase.signup: 회원가입 완료 - memberId: {}, isNewMember: {}",
                result.member().getMemberId(), result.isNew());

        // 4. 응답 DTO 생성 및 반환
        return SignupResponseDto.from(result.member());
    }

    /**
     * 회원가입 요청 데이터 검증
     *
     * @param request 회원가입 요청 DTO
     * @throws IllegalArgumentException 데이터가 유효하지 않을 때
     */
    private void validateSignupRequest(SignupRequestDto request) {
        // 이메일 검증
        if (request.getEmail() == null || request.getEmail().isBlank()) {
            log.warn("LocalAuthUseCase.validateSignupRequest: 이메일 없음");
            throw new IllegalArgumentException("이메일은 필수입니다");
        }

        // 비밀번호 검증
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            log.warn("LocalAuthUseCase.validateSignupRequest: 비밀번호 없음");
            throw new IllegalArgumentException("비밀번호는 필수입니다");
        }

        if (request.getPassword().length() < 8 || request.getPassword().length() > 50) {
            log.warn("LocalAuthUseCase.validateSignupRequest: 비밀번호 길이 초과");
            throw new IllegalArgumentException("비밀번호는 8~50자여야 합니다");
        }

        // 이름 검증
        if (request.getName() == null || request.getName().isBlank()) {
            log.warn("LocalAuthUseCase.validateSignupRequest: 이름 없음");
            throw new IllegalArgumentException("이름은 필수입니다");
        }

        if (request.getName().length() < 2 || request.getName().length() > 50) {
            log.warn("LocalAuthUseCase.validateSignupRequest: 이름 길이 초과");
            throw new IllegalArgumentException("이름은 2~50자여야 합니다");
        }

        log.debug("LocalAuthUseCase.validateSignupRequest: 검증 완료");
    }
}
