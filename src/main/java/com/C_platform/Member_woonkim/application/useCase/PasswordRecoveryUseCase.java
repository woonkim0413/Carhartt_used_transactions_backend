package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.domain.value.VerificationCode;
import com.C_platform.Member_woonkim.exception.EmailErrorCode;
import com.C_platform.Member_woonkim.exception.EmailException;
import com.C_platform.Member_woonkim.exception.LocalAuthErrorCode;
import com.C_platform.Member_woonkim.exception.LocalAuthException;
import com.C_platform.Member_woonkim.infrastructure.auth.cache.EmailVerificationCodeStore;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.Member_woonkim.infrastructure.mail.EmailService;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.PasswordFindRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.PasswordResetRequestDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SuccessMessageResponseDto;
import com.C_platform.annotation.UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 비밀번호 복구 UseCase
 *
 * 비밀번호 찾기 및 재설정 관련 비즈니스 로직을 처리합니다.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class PasswordRecoveryUseCase {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final EmailVerificationCodeStore codeStore;
    private final JavaMailSender javaMailSender;

    /**
     * 비밀번호 재설정을 위한 인증 코드 전송
     *
     * 1. 이메일 유효성 검증
     * 2. LOCAL 계정 존재 여부 확인
     * 3. 6자리 난수 코드 생성
     * 4. 코드 저장소에 저장 (TTL: 10분)
     * 5. 이메일로 코드 전송
     *
     * @param dto 비밀번호 찾기 요청 (이메일)
     * @return 성공 메시지
     * @throws LocalAuthException 가입되지 않은 이메일
     * @throws EmailException 이메일 전송 실패
     */
    public SuccessMessageResponseDto sendPasswordResetCode(PasswordFindRequestDto dto) {
        log.info("PasswordRecoveryUseCase.sendPasswordResetCode: 비밀번호 재설정 코드 전송 시작 - email: {}", dto.email());

        String email = dto.email().trim();

        // 1. 이메일 유효성 검증
        validateEmail(email);

        // 2. LOCAL 계정 존재 여부 확인
        Member member = memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
                .orElseThrow(() -> {
                    log.warn("PasswordRecoveryUseCase.sendPasswordResetCode: 가입되지 않은 이메일 - email: {}", email);
                    return new LocalAuthException(LocalAuthErrorCode.C002);
                });

        log.debug("PasswordRecoveryUseCase.sendPasswordResetCode: 회원 존재 확인 - memberId: {}", member.getMemberId());

        // 3. 6자리 난수 코드 생성
        VerificationCode verificationCode = VerificationCode.generate();
        String code = verificationCode.getValue();
        log.debug("PasswordRecoveryUseCase.sendPasswordResetCode: 인증 코드 생성 완료");
        log.debug("인증 코드 : [{}]", code);

        // 4. 코드 저장소에 저장 (TTL: 10분)
        codeStore.saveCode(email, code);
        log.debug("PasswordRecoveryUseCase.sendPasswordResetCode: 인증 코드 저장소에 저장 완료");

        // 5. 이메일로 코드 전송
        try {
            emailService.sendVerificationCodeEmail(email, code, javaMailSender);
            log.info("PasswordRecoveryUseCase.sendPasswordResetCode: 비밀번호 재설정 코드 전송 완료 - email: {}", email);
        } catch (EmailException e) {
            log.error("PasswordRecoveryUseCase.sendPasswordResetCode: 이메일 전송 실패 - email: {}", email);
            throw e;
        }

        return new SuccessMessageResponseDto("인증 코드가 발송되었습니다");
    }

    /**
     * 인증 코드 검증 및 비밀번호 재설정
     *
     * 1. 이메일 유효성 검증
     * 2. LOCAL 계정 존재 여부 확인
     * 3. 저장소에서 코드 조회 (없으면 E002 예외)
     * 4. 입력 코드와 저장 코드 비교 (불일치 시 E003 예외)
     * 5. 새 비밀번호 BCrypt 암호화
     * 6. 데이터베이스 회원 비밀번호 업데이트
     * 7. 인증 코드 저장소에서 코드 삭제 (또는 TTL 기반 자동 삭제)
     *
     * @param dto 비밀번호 재설정 요청 (이메일, 코드, 새 비밀번호)
     * @return 성공 메시지
     * @throws LocalAuthException 가입되지 않은 이메일
     * @throws EmailException 코드 만료, 코드 불일치
     */
    public SuccessMessageResponseDto resetPassword(PasswordResetRequestDto dto) {
        log.info("PasswordRecoveryUseCase.resetPassword: 비밀번호 재설정 시작 - email: {}", dto.email());

        String email = dto.email().trim();
        String code = dto.code().trim();
        String newPassword = dto.newPassword();

        // 1. 이메일 유효성 검증
        validateEmail(email);

        // 2. LOCAL 계정 존재 여부 확인
        Member member = memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
                .orElseThrow(() -> {
                    log.warn("PasswordRecoveryUseCase.resetPassword: 가입되지 않은 이메일 - email: {}", email);
                    return new LocalAuthException(LocalAuthErrorCode.C002);
                });

        log.debug("PasswordRecoveryUseCase.resetPassword: 회원 존재 확인 - memberId: {}", member.getMemberId());

        // 3. 저장소에서 코드 조회 (없으면 E002 예외)
        String storedCode = codeStore.getCode(email)
                .orElseThrow(() -> {
                    log.warn("PasswordRecoveryUseCase.resetPassword: 인증 코드 만료 또는 없음 - email: {}", email);
                    return new EmailException(EmailErrorCode.E002);
                });

        // 4. 입력 코드와 저장 코드 비교 (불일치 시 E003 예외)
        if (!code.equals(storedCode)) {
            log.warn("PasswordRecoveryUseCase.resetPassword: 인증 코드 불일치 - email: {}", email);
            throw new EmailException(EmailErrorCode.E003);
        }

        log.debug("PasswordRecoveryUseCase.resetPassword: 인증 코드 검증 완료");

        // 5. 새 비밀번호 BCrypt 암호화
        log.info("새 비밀번호 : {}", newPassword);
        String encodedPassword = passwordEncoder.encode(newPassword);
        log.info("PasswordRecoveryUseCase.resetPassword: 새 비밀번호 암호화 완료");

        // 6. 데이터베이스 회원 비밀번호 업데이트
        member.changePassword(encodedPassword);
        memberRepository.save(member);
        log.info("PasswordRecoveryUseCase.resetPassword: 비밀번호 업데이트 완료 - memberId: {}", member.getMemberId());

        // 7. 인증 코드 저장소에서 코드 삭제 (TTL 기반으로 자동 삭제되므로 명시적 삭제는 선택사항)
        // 향후 필요시 codeStore.removeCode(email) 메서드 추가 가능

        return new SuccessMessageResponseDto("비밀번호가 성공적으로 변경되었습니다");
    }

    /**
     * 이메일 유효성 검증
     *
     * @param email 이메일 주소
     * @throws IllegalArgumentException 유효하지 않은 이메일
     */
    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            log.warn("PasswordRecoveryUseCase.validateEmail: 이메일 공백");
            throw new IllegalArgumentException("이메일은 필수입니다");
        }

        // 간단한 이메일 형식 검증 (@ 포함 여부)
        if (!email.contains("@")) {
            log.warn("PasswordRecoveryUseCase.validateEmail: 유효하지 않은 이메일 형식");
            throw new IllegalArgumentException("유효한 이메일 형식이 아닙니다");
        }

        log.debug("PasswordRecoveryUseCase.validateEmail: 이메일 검증 완료");
    }
}
