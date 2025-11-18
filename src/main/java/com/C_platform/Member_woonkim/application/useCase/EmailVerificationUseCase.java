package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.annotation.UseCase;
import com.C_platform.Member_woonkim.domain.value.VerificationCode;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.exception.EmailException;
import com.C_platform.Member_woonkim.exception.EmailErrorCode;
import com.C_platform.Member_woonkim.infrastructure.auth.cache.EmailVerificationCodeStore;
import com.C_platform.Member_woonkim.infrastructure.mail.EmailService;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SendRandomCodeToEmailDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.RandomCodeVerificationDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SuccessMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;

/**
 * 이메일 검증 사용 사례
 * 인증 코드 전송 및 검증을 담당합니다.
 */
@UseCase
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationUseCase {

    private final EmailService emailService;
    // Email과 expiredTime을 저장하는 객체다 (내부에 time Scheduler 존재)
    private final EmailVerificationCodeStore codeStore;
    private final MemberRepository memberRepository;
    // 메일을 보내주는 객체 (객체가 알아서 환경 변수에서 Gmail Auth Email/Passowrd를 읽는다)
    private final JavaMailSender javaMailSender;

    /**
     * 이메일로 인증 코드 전송
     * @param dto 이메일 정보
     * @return 성공 메시지
     * @throws EmailException 메일 전송 실패 또는 중복 이메일
     */
    public SuccessMessageResponseDto sendVerificationCode(SendRandomCodeToEmailDto dto) {

        String email = dto.email().trim();

        // 1. 이메일 형식 검증 (DTO @Valid에서 처리되지만 명시적으로 재확인)
        validateEmail(email);

        // 2. 이미 가입된 회원인지 확인
        if (memberRepository
                .findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
                .isPresent()) {
            throw new EmailException(EmailErrorCode.E005);
        }

        // 3. 6자리 난수 코드 생성 (난수 값을 가지고 있는 VO 생성)
        VerificationCode code = VerificationCode.generate();
        log.info("[디버깅 목적] 생성한 난수 값: {}", code.getValue());

        // 4. Memory에 저장 (TTL: 10분) (나중에 Redis로 전환)
        codeStore.saveCode(email, code.getValue());

        // 5. 메일 전송
        emailService.sendVerificationCodeEmail(email, code.getValue(), javaMailSender);

        log.info("Verification code sent to email: {}", email);

        return new SuccessMessageResponseDto("인증 코드가 발송되었습니다");
    }

    /**
     * 인증 코드 검증
     * @param dto 이메일과 코드
     * @return 성공 메시지
     * @throws EmailException 코드 만료 또는 불일치
     */
    public SuccessMessageResponseDto verifyCode(RandomCodeVerificationDto dto) {

        String email = dto.email().trim();
        String inputCode = dto.code().trim();

        // 1. 이메일 형식 검증
        validateEmail(email);

        // 2. 서버 저장소에서 저장된 코드 조회 및 삭제
        // .orElseThreow()의 method heading은 아래와 같다 (Optional<T>의 T를 반환한다)
        // public <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {}
        String savedCode = codeStore.getCode(email)
            .orElseThrow(() -> {
                log.warn("Verification code expired for email: {}", email);
                return new EmailException(EmailErrorCode.E002);
            });

        // 3. 코드 일치 여부 확인
        if (!savedCode.equals(inputCode)) {
            log.warn("Verification code mismatch for email: {}", email);
            throw new EmailException(EmailErrorCode.E003);
        }

        // 4. 검증 완료 표시 (signup 시 확인용)
        codeStore.markAsVerified(email);

        log.info("Email verified successfully: {}", email);

        return new SuccessMessageResponseDto("이메일 인증이 완료되었습니다");
    }

    /**
     * 이메일 형식 검증
     * @param email 이메일 주소
     * @throws EmailException 형식이 올바르지 않은 경우
     */
    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new EmailException(EmailErrorCode.E004);
        }

        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$";
        if (!email.matches(emailRegex)) {
            throw new EmailException(EmailErrorCode.E004);
        }
    }
}
