# 📧 이메일 검증 기능 설계 문서

## 1. 개요
Local 로그인 시스템에 **이메일 검증(Email Verification)** 기능을 추가합니다.
사용자가 가입할 때 이메일로 전송된 6자리 난수 인증 코드를 검증하여 유효한 이메일 주소를 확인합니다.

---

## 2. 요구사항 정리

| 항목 | 내용 |
|------|------|
| **인증 코드 저장소** | Redis (분산 시스템 대응) |
| **코드 유효 시간** | 10분 (600초) |
| **코드 형식** | 6자리 숫자 (000000 ~ 999999) |
| **사용 시점** | Signup 단계에서만 사용 (로그인 미사용) |
| **메일 서버** | Gmail SMTP (이미 설정됨) |

---

## 3. API 엔드포인트 설계

### 3.1 이메일 인증 코드 전송
```
POST /v1/local/email/random_code

Request:
{
  "email": "user@example.com"
}

Response (Success - 200):
{
  "success": true,
  "data": {
    "successMessage": "인증 코드가 발송되었습니다"
  },
  "error": null,
  "metaData": {
    "timestamp": "2025-11-12T10:30:00Z",
    "requestId": "..."
  }
}

Response (Error - 400/409):
{
  "success": false,
  "data": null,
  "error": {
    "code": "E004",
    "message": "유효하지 않은 이메일입니다"
  },
  "metaData": {
    "timestamp": "2025-11-12T10:30:00Z",
    "requestId": "..."
  }
}
```

**처리 로직:**
1. 이메일 형식 검증 (@NotBlank, @Email)
2. 이미 가입된 회원 확인 (findByLocalProviderAndEmail)
3. 6자리 난수 코드 생성 (SecureRandom)
4. Redis에 저장 (key: `verification:email`, TTL: 600초)
5. Gmail SMTP로 메일 전송
6. 성공 응답 반환

---

### 3.2 인증 코드 검증
```
POST /v1/local/email/verification

Request:
{
  "email": "user@example.com",
  "code": "123456"
}

Response (Success - 200):
{
  "success": true,
  "data": {
    "successMessage": "이메일 인증이 완료되었습니다"
  },
  "error": null,
  "metaData": {
    "timestamp": "2025-11-12T10:30:00Z",
    "requestId": "..."
  }
}

Response (Error - 400):
{
  "success": false,
  "data": null,
  "error": {
    "code": "E003",
    "message": "인증 코드가 일치하지 않습니다"
  },
  "metaData": {
    "timestamp": "2025-11-12T10:30:00Z",
    "requestId": "..."
  }
}
```

**처리 로직:**
1. 이메일 형식 검증
2. Redis에서 저장된 코드 조회 (GET + DEL)
3. 코드 없음 → E002 에러 (만료됨)
4. 코드 불일치 → E003 에러
5. 코드 일치 → Redis에 `verified:email` 저장
6. 성공 응답 반환

---

## 4. 에러 코드 정의

LocalAuthErrorCode에 다음 4개의 이메일 검증 에러 코드 추가:

```java
E001("E001", "이메일로 코드 전송 실패했습니다"),        // 메일 서버 오류
E002("E002", "인증 코드가 만료되었습니다"),            // Redis에 코드 없음
E003("E003", "인증 코드가 일치하지 않습니다"),        // 입력 코드 != 저장 코드
E004("E004", "유효하지 않은 이메일입니다"),            // 이미 가입된 회원
```

---

## 5. 아키텍처 설계

### 5.1 Domain Layer

#### `VerificationCode` (Value Object)
```java
package com.C_platform.Member_woonkim.domain.value;

public class VerificationCode {
    private final String code;

    private VerificationCode(String code) {
        this.code = code;
    }

    /**
     * 6자리 난수 코드 생성
     * @return VerificationCode 객체
     */
    public static VerificationCode generate() {
        SecureRandom random = new SecureRandom();
        int randomNumber = random.nextInt(1000000); // 0 ~ 999999
        String code = String.format("%06d", randomNumber);
        return new VerificationCode(code);
    }

    /**
     * 코드 값 반환
     */
    public String getValue() {
        return code;
    }

    /**
     * 6자리 숫자 형식 검증
     */
    public boolean isValid() {
        return code.matches("^\\d{6}$");
    }

    @Override
    public String toString() {
        return code;
    }
}
```

**특징:**
- 불변 객체 (immutable)
- 생성 시점에만 난수 생성
- 범위: 000000 ~ 999999

---

### 5.2 Infrastructure Layer

#### `EmailService` (메일 전송)
```java
package com.C_platform.Member_woonkim.infrastructure.mail;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender javaMailSender;
    private static final String FROM_EMAIL = "gamegemos588@gmail.com";
    private static final String SUBJECT = "Carhartt 이메일 인증 코드";

    /**
     * 이메일로 인증 코드 전송
     * @param to 수신 이메일
     * @param code 6자리 난수 코드
     * @throws EmailException 메일 전송 실패 시
     */
    public void sendVerificationCodeEmail(String to, String code)
            throws EmailException {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(FROM_EMAIL);
            message.setTo(to);
            message.setSubject(SUBJECT);
            message.setText(buildEmailContent(code));

            javaMailSender.send(message);
            log.info("Verification code sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send verification code to {}", to, e);
            throw new EmailException(EmailErrorCode.E001);
        }
    }

    /**
     * 이메일 본문 구성
     */
    private String buildEmailContent(String code) {
        return String.format(
            "안녕하세요,\n\n" +
            "Carhartt 회원가입을 위한 인증 코드입니다.\n\n" +
            "인증 코드: %s\n\n" +
            "이 코드는 10분 동안 유효합니다.\n" +
            "본인이 요청하지 않았다면 이 이메일을 무시해주세요.\n\n" +
            "감사합니다.",
            code
        );
    }
}
```

**특징:**
- JavaMailSender Bean 주입 (Spring Boot가 자동 생성)
- 메일 전송 실패 시 EmailException 발생
- 로깅 추가 (추적성)

---

#### `EmailVerificationCodeStore` (Redis 저장소)
```java
package com.C_platform.Member_woonkim.infrastructure.cache;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class EmailVerificationCodeStore {

    private final StringRedisTemplate redisTemplate;
    private static final String KEY_PREFIX = "verification:";
    private static final long TTL_SECONDS = 600; // 10분

    /**
     * 인증 코드를 Redis에 저장 (TTL: 10분)
     * @param email 이메일 주소
     * @param code 인증 코드
     */
    public void saveCode(String email, String code) {
        String key = buildKey(email);
        redisTemplate.opsForValue().set(
            key,
            code,
            TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * Redis에서 코드 조회 및 삭제
     * @param email 이메일 주소
     * @return 저장된 코드 (없거나 만료된 경우 Optional.empty())
     */
    public Optional<String> getAndDeleteCode(String email) {
        String key = buildKey(email);
        String code = redisTemplate.opsForValue().getAndDelete(key);
        return Optional.ofNullable(code);
    }

    /**
     * 검증된 이메일 표시 (signup 시 확인용)
     * @param email 이메일 주소
     */
    public void markAsVerified(String email) {
        String key = buildVerifiedKey(email);
        redisTemplate.opsForValue().set(
            key,
            "true",
            TTL_SECONDS,
            TimeUnit.SECONDS
        );
    }

    /**
     * 검증 여부 확인
     * @param email 이메일 주소
     * @return 검증 여부
     */
    public boolean isVerified(String email) {
        String key = buildVerifiedKey(email);
        return Boolean.TRUE.equals(
            redisTemplate.hasKey(key)
        );
    }

    private String buildKey(String email) {
        return KEY_PREFIX + email;
    }

    private String buildVerifiedKey(String email) {
        return KEY_PREFIX + "verified:" + email;
    }
}
```

**특징:**
- SETEX 명령으로 원자적 설정 + TTL
- GET + DEL로 원자적 조회 및 삭제
- verified 상태 별도 관리 (signup과 통합 시 사용)

---

#### `EmailException` (커스텀 예외)
```java
package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.Getter;

@Getter
public class EmailException extends RuntimeException {
    private final ErrorCode errorCode;

    public EmailException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

---

#### `EmailErrorCode` (에러 코드 열거형)
```java
package com.C_platform.Member_woonkim.exception;

import com.C_platform.global.error.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum EmailErrorCode implements ErrorCode {
    E001("E001", "이메일로 코드 전송 실패했습니다"),
    E002("E002", "인증 코드가 만료되었습니다"),
    E003("E003", "인증 코드가 일치하지 않습니다"),
    E004("E004", "유효하지 않은 이메일입니다");

    private final String code;
    private final String message;

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
```

---

### 5.3 Application Layer

#### `EmailVerificationUseCase`
```java
package com.C_platform.Member_woonkim.application.useCase;

import com.C_platform.annotation.UseCase;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.value.VerificationCode;
import com.C_platform.Member_woonkim.exception.EmailException;
import com.C_platform.Member_woonkim.exception.EmailErrorCode;
import com.C_platform.Member_woonkim.infrastructure.cache.EmailVerificationCodeStore;
import com.C_platform.Member_woonkim.infrastructure.mail.EmailService;
import com.C_platform.Member_woonkim.infrastructure.repository.MemberRepository;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.SendRandomCodeToEmailDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.request.RandomCodeVerificationDto;
import com.C_platform.Member_woonkim.presentation.dto.Local.response.SuccessMessageResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.C_platform.Member_woonkim.domain.value.LocalProvider;

@UseCase
@RequiredArgsConstructor
@Slf4j
public class EmailVerificationUseCase {

    private final EmailService emailService;
    private final EmailVerificationCodeStore codeStore;
    private final MemberRepository memberRepository;

    /**
     * 이메일로 인증 코드 전송
     * @param dto 이메일 정보
     * @return 성공 메시지
     * @throws EmailException 메일 전송 실패 또는 중복 이메일
     */
    public SuccessMessageResponseDto sendVerificationCode(
            SendRandomCodeToEmailDto dto) {

        String email = dto.email().trim();

        // 1. 이메일 형식 검증 (DTO @Valid에서 처리되지만 명시적으로 재확인)
        validateEmail(email);

        // 2. 이미 가입된 회원인지 확인
        if (memberRepository
                .findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
                .isPresent()) {
            throw new EmailException(EmailErrorCode.E004);
        }

        // 3. 6자리 난수 코드 생성
        VerificationCode code = VerificationCode.generate();

        // 4. Redis에 저장 (TTL: 10분)
        codeStore.saveCode(email, code.getValue());

        // 5. 메일 전송
        emailService.sendVerificationCodeEmail(email, code.getValue());

        log.info("Verification code sent to email: {}", email);

        return new SuccessMessageResponseDto("인증 코드가 발송되었습니다");
    }

    /**
     * 인증 코드 검증
     * @param dto 이메일과 코드
     * @return 성공 메시지
     * @throws EmailException 코드 만료 또는 불일치
     */
    public SuccessMessageResponseDto verifyCode(
            RandomCodeVerificationDto dto) {

        String email = dto.email().trim();
        String inputCode = dto.code().trim();

        // 1. 이메일 형식 검증
        validateEmail(email);

        // 2. Redis에서 저장된 코드 조회 및 삭제
        String savedCode = codeStore.getAndDeleteCode(email)
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
```

**특징:**
- @UseCase 어노테이션 (스프링 컴포넌트)
- 명확한 단계별 처리 (로깅 포함)
- 예외 처리 (메일 전송 실패, 코드 만료, 불일치)

---

### 5.4 Presentation Layer

#### `LocalAuthController` (기존 파일에 추가)
```java
// 기존 코드 생략...

@RestController
@RequestMapping("/v1/local")
@RequiredArgsConstructor
public class LocalAuthController {

    private final LocalAuthUseCase localAuthUseCase;
    private final EmailVerificationUseCase emailVerificationUseCase;

    // 기존 엔드포인트: signup, login, check, logout...

    /**
     * 이메일로 인증 코드 전송
     */
    @PostMapping("/email/random_code")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> sendRandomCodeToEmail(
            @Valid @RequestBody SendRandomCodeToEmailDto dto) {
        SuccessMessageResponseDto response =
            emailVerificationUseCase.sendVerificationCode(dto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 인증 코드 검증
     */
    @PostMapping("/email/verification")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> randomCodeVerification(
            @Valid @RequestBody RandomCodeVerificationDto dto) {
        SuccessMessageResponseDto response =
            emailVerificationUseCase.verifyCode(dto);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
```

**주의:**
- HTTP 메서드를 POST로 변경 (원래 GET은 부적절)
- @Valid로 DTO 검증 활성화

---

### 5.5 DTO (기존 파일 활용)

#### `SendRandomCodeToEmailDto`
```java
package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record SendRandomCodeToEmailDto(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email
) {}
```

#### `RandomCodeVerificationDto`
```java
package com.C_platform.Member_woonkim.presentation.dto.Local.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RandomCodeVerificationDto(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "인증 코드는 필수입니다")
    String code
) {}
```

#### `SuccessMessageResponseDto`
```java
package com.C_platform.Member_woonkim.presentation.dto.Local.response;

public record SuccessMessageResponseDto(
    String successMessage
) {}
```

---

## 6. 설정 변경사항

### 6.1 `build.gradle` (Redis 의존성 추가)
```gradle
// 기존 dependencies...

dependencies {
    // ... 기존 의존성 ...

    // Redis
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### 6.2 `application.properties` (Redis 설정 추가)
```properties
# ... 기존 설정 ...

# Redis Configuration
spring.data.redis.host=localhost
spring.data.redis.port=6379
spring.data.redis.timeout=60000ms
spring.data.redis.jedis.pool.max-active=8
spring.data.redis.jedis.pool.max-idle=8
spring.data.redis.jedis.pool.min-idle=0
```

---

## 7. 데이터 흐름

### 7.1 이메일 코드 전송 플로우
```
Client
  ↓
POST /v1/local/email/random_code
  ↓
LocalAuthController.sendRandomCodeToEmail()
  ↓
EmailVerificationUseCase.sendVerificationCode()
  ├─ 이메일 형식 검증
  ├─ 중복 회원 확인 (MemberRepository)
  ├─ 코드 생성 (VerificationCode.generate())
  ├─ Redis 저장 (EmailVerificationCodeStore.saveCode)
  ├─ 메일 전송 (EmailService.sendVerificationCodeEmail)
  └─ 성공 응답
  ↓
Client (메일 수신)
```

### 7.2 인증 코드 검증 플로우
```
Client
  ↓
POST /v1/local/email/verification
  ↓
LocalAuthController.randomCodeVerification()
  ↓
EmailVerificationUseCase.verifyCode()
  ├─ 이메일 형식 검증
  ├─ Redis 코드 조회 (EmailVerificationCodeStore.getAndDeleteCode)
  │  └─ 없으면 E002 에러 (만료)
  ├─ 코드 비교
  │  └─ 불일치하면 E003 에러
  ├─ verified 표시 (EmailVerificationCodeStore.markAsVerified)
  └─ 성공 응답
  ↓
Client
```

---

## 8. 향후 Signup 통합 (선택사항)

현재는 이메일 검증이 독립적이며, 향후 signup과 통합하려면:

```java
// LocalAuthUseCase.signup() 수정 예시
public SignupResponseDto signup(SignupRequestDto request) {
    validateSignupRequest(request);

    // 이메일 검증 확인 추가
    if (!emailVerificationCodeStore.isVerified(request.getEmail())) {
        throw new SignupException(SignupErrorCode.EMAIL_NOT_VERIFIED);
    }

    String encodedPassword = passwordEncoder.encode(request.getPassword());

    JoinOrLoginResult result = memberJoinService.ensureLocalMember(
        LocalProvider.LOCAL,
        request.getEmail(),
        encodedPassword,
        request.getName()
    );

    // 검증 완료 표시 제거
    emailVerificationCodeStore.clearVerification(request.getEmail());

    return SignupResponseDto.from(result.member());
}
```

---

## 9. 보안 고려사항

1. **Rate Limiting:** 같은 이메일로 반복 요청 방지 (Redis INCR 사용)
2. **CSRF 보호:** SecurityConfig에서 `/v1/local/email/*` 미제외 (CSRF 검증)
3. **이메일 가리기:** 응답에서 이메일 주소 완전 노출 방지
4. **코드 브루트포스:** 코드 5회 실패 시 일시 잠금
5. **로깅:** 민감 정보(코드 값) 로그에 기록하지 않음

---

## 10. 테스트 전략

### 10.1 단위 테스트 (VerificationCode)
```
- generate(): 형식이 올바른 6자리 숫자인지 확인
- isValid(): 유효한 형식 검증
```

### 10.2 통합 테스트 (EmailVerificationUseCase)
```
- sendVerificationCode():
  * 정상 전송 (메일 발송 확인)
  * 중복 이메일 (E004 에러)
  * 유효하지 않은 이메일 (E004 에러)

- verifyCode():
  * 정상 검증 (코드 일치)
  * 만료된 코드 (E002 에러)
  * 불일치 코드 (E003 에러)
```

### 10.3 E2E 테스트
```
- POST /v1/local/email/random_code → POST /v1/local/email/verification 통합 플로우
```

---

## 11. 파일 생성/수정 목록

| 작업 | 파일 경로 | 내용 |
|------|----------|------|
| 신규 | `domain/value/VerificationCode.java` | 값 객체 |
| 신규 | `infrastructure/mail/EmailService.java` | 메일 전송 서비스 |
| 신규 | `infrastructure/cache/EmailVerificationCodeStore.java` | Redis 저장소 |
| 신규 | `exception/EmailException.java` | 커스텀 예외 |
| 신규 | `exception/EmailErrorCode.java` | 에러 코드 |
| 신규 | `application/useCase/EmailVerificationUseCase.java` | 비즈니스 로직 |
| 수정 | `presentation/controller/LocalAuthController.java` | 2개 엔드포인트 추가 |
| 수정 | `presentation/dto/Local/request/SendRandomCodeToEmailDto.java` | 이미 생성됨 |
| 수정 | `presentation/dto/Local/request/RandomCodeVerificationDto.java` | 이미 생성됨 |
| 수정 | `presentation/dto/Local/response/SuccessMessageResponseDto.java` | 이미 생성됨 |
| 수정 | `build.gradle` | Redis 의존성 추가 |
| 수정 | `application.properties` | Redis 설정 추가 |

---

## 12. 구현 순서

1. **의존성 추가** → build.gradle (Redis)
2. **Redis 설정** → application.properties
3. **Domain 계층** → VerificationCode.java
4. **Exception 계층** → EmailException.java, EmailErrorCode.java
5. **Infrastructure 계층** → EmailService.java, EmailVerificationCodeStore.java
6. **Application 계층** → EmailVerificationUseCase.java
7. **Presentation 계층** → LocalAuthController.java 수정
8. **테스트** → 단위/통합/E2E 테스트

---

## 13. 주의사항

- ⚠️ **Redis 설치 필수:** 로컬 개발 환경에 Redis 설치 필요
- ⚠️ **메일 설정:** application.properties의 Gmail 설정 확인
- ⚠️ **HTTP 메서드:** 원래 요청서의 @GetMapping → @PostMapping 변경
- ⚠️ **TTL:** 10분(600초) 설정, 필요시 조정 가능
