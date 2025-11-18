# 📧 이메일 검증 기능 설계 문서

## 1. 개요
Local 로그인 시스템에 **이메일 검증(Email Verification)** 기능을 추가합니다.
사용자가 가입할 때 이메일로 전송된 6자리 난수 인증 코드를 검증하여 유효한 이메일 주소를 확인합니다.

---

## 2. 요구사항 정리

| 항목 | 내용                        |
|------|---------------------------|
| **인증 코드 저장소** | Redis (분산 시스템 대응)         |
| **코드 유효 시간** | 5분 (600초)                 |
| **코드 형식** | 6자리 숫자 (000000 ~ 999999)  |
| **사용 시점** | Signup 단계에서만 사용 (로그인 미사용) |
| **메일 서버** | Gmail SMTP (이미 설정됨)       |

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

### 6.1 `build.gradle` (Redis 의존성 선택사항)
```gradle
// 기존 dependencies...

dependencies {
    // ... 기존 의존성 ...

    // Redis는 선택사항 (로컬 개발 환경에서는 메모리 기반 저장소 사용)
    // implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

### 6.2 `application.properties` (Redis 설정 선택사항)
```properties
# ... 기존 설정 ...

# Redis Configuration (Optional - 로컬 개발 환경에서는 메모리 기반 저장소 사용)
# spring.data.redis.host=localhost
# spring.data.redis.port=6379
# spring.data.redis.timeout=60000ms
# spring.data.redis.jedis.pool.max-active=8
# spring.data.redis.jedis.pool.max-idle=8
# spring.data.redis.jedis.pool.min-idle=0
```

**참고:** Redis가 필요한 경우 위 설정을 주석해제하고 Redis 서버를 실행하면 됩니다.

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
  ├─ 메모리 저장 (EmailVerificationCodeStore.saveCode)
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
  ├─ 메모리 코드 조회 (EmailVerificationCodeStore.getAndDeleteCode)
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

---

## 14. 구현 완료

### 14.1 생성된 파일 목록

| 파일 경로 | 설명 |
|---------|------|
| `domain/value/VerificationCode.java` | 6자리 난수 코드 생성 및 검증 값 객체 |
| `exception/EmailException.java` | 이메일 검증 커스텀 예외 |
| `exception/EmailErrorCode.java` | 이메일 검증 에러 코드 열거형 (E001-E004) |
| `infrastructure/mail/EmailService.java` | Gmail SMTP 메일 발송 서비스 |
| `infrastructure/cache/EmailVerificationCodeStore.java` | Redis 기반 인증 코드 저장소 |
| `application/useCase/EmailVerificationUseCase.java` | 이메일 검증 비즈니스 로직 |

### 14.2 수정된 파일 목록

| 파일 경로 | 수정 사항 |
|---------|---------|
| `build.gradle` | Redis 의존성 주석 처리 (선택사항) |
| `application.properties` | Redis 설정 주석 처리 (선택사항) |
| `presentation/dto/Local/request/RandomCodeVerificationDto.java` | email, code 필드 구조로 변경 |
| `presentation/controller/LocalAuthController.java` | 이메일 검증 엔드포인트 2개 추가 (@PostMapping) |

### 14.3 주요 구현 특징

1. **6자리 난수 생성:** SecureRandom을 사용하여 안전한 난수 생성
2. **메모리 기반 저장:** ConcurrentHashMap을 사용한 스레드 안전 저장소
3. **자동 만료 관리:** 10분(600초) TTL과 백그라운드 정리 스레드
4. **원자적 조회 및 삭제:** remove() 메서드로 안전한 코드 사용 (중복 사용 방지)
5. **명확한 에러 코드:** 4가지 에러 상황 분류 (E001-E004)
6. **입력 검증:** DTO의 @Valid와 UseCase 내 명시적 검증
7. **로깅:** 각 단계별 추적 로깅으로 디버깅 용이
8. **Google SMTP:** 기존 Gmail 설정 활용
9. **Redis 선택사항:** 로컬 개발에서는 메모리 사용, 프로덕션에서는 Redis 사용 가능

---

## 15. Redis 의존성 제거 이유 및 메모리 기반 저장소로 변경

### 15.1 변경 배경

원래 Redis를 사용하여 인증 코드를 분산 시스템에 저장하려고 했지만, 로컬 개발 환경에서 Redis 연결 실패 문제가 발생했습니다.

**에러 메시지:**
```
java.net.ConnectException: Connection refused: getsockopt
org.springframework.data.redis.RedisConnectionFailureException: Unable to connect to Redis
```

### 15.2 해결 방안

Redis에 대한 의존성을 제거하고 메모리 기반 저장소(`EmailVerificationCodeStore`)로 변경하였습니다.

**장점:**
- 로컬 개발 환경에서 Redis 설치 불필요
- 빠른 개발 및 테스트 가능
- 단일 서버 환경에서 충분한 성능
- 외부 의존성 감소

**구현 세부사항:**
- `ConcurrentHashMap`을 사용한 스레드 안전 저장소
- `ScheduledExecutorService`를 사용한 백그라운드 정리 스레드
- 1분 주기로 만료된 항목 자동 제거
- 메모리 누수 방지

### 15.3 프로덕션 환경 (선택사항)

프로덕션 환경에서 분산 시스템을 지원하려면:

```gradle
dependencies {
    // Redis 의존성 주석해제
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
}
```

```properties
# Redis 설정 주석해제
spring.data.redis.host=redis-server-host
spring.data.redis.port=6379
spring.data.redis.password=your-password (필요시)
```

그 후 `EmailVerificationCodeStore`를 Redis 기반 구현으로 변경하면 됩니다.
(원본 Redis 구현 코드는 git history에서 확인 가능)

---

## 16. 에러 분석 및 해결 (2025-11-12 발생)

### 16.1 에러 로그 분석

**발생 에러:**
```
org.springframework.mail.MailAuthenticationException: Authentication failed
jakarta.mail.AuthenticationFailedException: 535-5.7.8 Username and Password not accepted
```

**원인:**
Gmail SMTP 인증 실패 - 일반 비밀번호로 2단계 인증이 활성화된 Gmail 계정에 접근 불가

**에러 발생 시점:**
- `POST /v1/local/email/random_code` 엔드포인트 호출 시
- `EmailService.sendVerificationCodeEmail()` → `JavaMailSender.send()` 에서 SMTP 연결 시도

---

### 16.2 문제 원인

Gmail은 보안 정책상 다음과 같은 상황에서 일반 비밀번호 접근을 거부합니다:

1. **2단계 인증 활성화 상태에서 일반 비밀번호 사용**
   - Gmail 계정에 2단계 인증이 활성화됨
   - 써드파티 앱 접근을 위해서는 **앱 비밀번호(App Password)** 필요

2. **현재 설정 상태:**
   ```properties
   spring.mail.password=hivomevsyumxdbzi  # ❌ 일반 비밀번호 (작동하지 않음)
   ```

3. **Gmail SMTP 요구사항:**
   - Host: `smtp.gmail.com` ✓
   - Port: `587` (TLS) ✓
   - Username: 유효한 Gmail 주소 ✓
   - Password: **앱 비밀번호 필수** ❌

---

### 16.3 해결 방법

#### Step 1: Gmail App Password 생성

1. Gmail 계정 접속: https://myaccount.google.com
2. 좌측 메뉴에서 **"보안"(Security)** 클릭
3. **"앱 비밀번호"(App passwords)** 선택
4. **앱**: Mail / **기기**: Windows PC 선택
5. **생성된 16자리 비밀번호 복사** (예: `xxxx xxxx xxxx xxxx`)

#### Step 2: application.properties 업데이트

```properties
# 변경 전
spring.mail.password=hivomevsyumxdbzi

# 변경 후 (생성한 앱 비밀번호, 공백 제거)
spring.mail.password=xxxxxxxxxxxxxxxx
```

**중요:** 생성된 비밀번호의 공백을 모두 제거하고 입력해야 합니다.

#### Step 3: 추가 설정 개선

```properties
spring.mail.properties.mail.smtp.ssl.protocols=TLSv1.2
```

TLSv1.2 명시적 설정으로 SSL/TLS 호환성 강화

---

### 16.4 적용 이후 테스트

```bash
# 1. 애플리케이션 재시작
./gradlew bootRun

# 2. 이메일 인증 코드 전송 테스트
curl -X POST http://localhost:8080/v1/local/email/random_code \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com"}'

# 3. 예상 응답
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
```

---

### 16.5 Gmail App Password가 보이지 않는 경우

**원인:** 2단계 인증이 활성화되지 않음

**해결:**
1. Gmail 보안 설정에서 **2단계 인증 활성화**
2. 전화번호 인증 완료
3. 그 후 "앱 비밀번호" 옵션이 나타남

---

### 16.6 보안 고려사항

⚠️ **비밀번호 관리:**
- 일반 환경: `application.properties`에 하드코딩 (로컬 개발용)
- 프로덕션: 환경 변수 또는 보안 저장소(AWS Secrets Manager, HashiCorp Vault) 사용 권장

```properties
# 프로덕션 권장 설정 (환경 변수)
spring.mail.password=${GMAIL_APP_PASSWORD}
```

---

### 16.7 참고 자료

- [Google Support: App passwords](https://support.google.com/mail/?p=BadCredentials)
- [Spring Boot Mail Configuration](https://spring.io/guides/gs/sending-email/)
- [Gmail SMTP Settings](https://support.google.com/mail/answer/7126229)

---

## 17. SecurityContext 세션 저장소 문제 해결 (2025-11-13 발생)

### 17.1 문제 상황

로그인 후 인증이 필요한 API (예: `GET /v1/local/check`)를 호출하면 UNAUTHORIZED 에러가 발생합니다.

**현상:**
```
POST /v1/local/login → 성공 (JSESSIONID 쿠키 반환)
GET /v1/local/check (JSESSIONID 쿠키 포함) → UNAUTHORIZED 에러 ❌
```

**에러 응답:**
```json
{
  "success": false,
  "error": "UNAUTHORIZED"
}
```

### 17.2 근본 원인

**SecurityContext가 HttpSession에 저장되지 않음!**

**기존 코드의 문제점:**

1. **SecurityConfig에서 SecurityContextRepository 미설정**
   - `SessionCreationPolicy.IF_REQUIRED`만 설정
   - 명시적인 `HttpSessionSecurityContextRepository` 빈 없음
   - 부모 클래스의 기본 동작에만 의존

2. **JsonUsernamePasswordAuthenticationFilter에서 위임만 수행**
   ```java
   // 이전 코드
   @Override
   protected void successfulAuthentication(...) {
       super.successfulAuthentication(request, response, chain, authResult);
       // 부모 클래스에만 위임 → SecurityContext가 세션에 저장되지 않음
   }
   ```

3. **결과: 인증 상태가 세션에 저장되지 않음**
   - 로그인 후 각 요청은 새로운 SecurityContext로 시작
   - JSESSIONID 쿠키가 있어도 세션에 인증 정보가 없음
   - 다음 요청에서 UNAUTHORIZED 에러

### 17.3 해결 방법

#### Step 1: SecurityConfig.java 수정

**1-1. Import 추가**
```java
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
```

**1-2. SecurityContextRepository 빈 등록**
```java
@Bean
public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
}
```

**1-3. securityFilterChain에서 명시적으로 설정**
```java
@Bean
public SecurityFilterChain securityFilterChain(
    HttpSecurity http,
    ...,
    SecurityContextRepository securityContextRepository) throws Exception {

    // 🔧 명시적으로 SecurityContextRepository 설정
    http.securityContext(securityContext ->
        securityContext.securityContextRepository(securityContextRepository)
    );

    // 나머지 설정...
}
```

#### Step 2: JsonUsernamePasswordAuthenticationFilter.java 수정

**2-1. Import 추가**
```java
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
```

**2-2. successfulAuthentication() 메서드 수정**
```java
@Override
protected void successfulAuthentication(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain,
                                        Authentication authResult) throws IOException, ServletException {

    // 🔧 Step 1: SecurityContext 생성 및 Authentication 저장
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authResult);

    // 🔧 Step 2: SecurityContextHolder에 저장 (현재 스레드용)
    SecurityContextHolder.setContext(context);

    // 🔧 Step 3: HttpSessionSecurityContextRepository를 사용하여 세션에 명시적으로 저장
    // 이 단계가 없으면 다음 요청에서 세션에서 SecurityContext를 로드할 수 없음!
    SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
    repository.saveContext(context, request, response);

    log.info("SecurityContext를 세션에 저장 완료 - email: {}", authResult.getName());

    // 🔧 Step 4: 부모 클래스의 표준 처리 실행 (SuccessHandler 호출)
    super.successfulAuthentication(request, response, chain, authResult);
}
```

### 17.4 수정 후 인증 흐름

**로그인 요청:**
```
POST /v1/local/login
  ↓
JsonUsernamePasswordAuthenticationFilter.attemptAuthentication()
  ├─ JSON 파싱
  ├─ 이메일/비밀번호 검증 및 정제
  ├─ UsernamePasswordAuthenticationToken 생성
  └─ authenticationManager.authenticate() 호출

  ↓ (인증 성공)

JsonUsernamePasswordAuthenticationFilter.successfulAuthentication()
  ├─ 🔧 SecurityContext 생성
  ├─ 🔧 SecurityContextHolder에 저장
  ├─ 🔧 HttpSession에 명시적으로 저장 ← 핵심!
  ├─ super.successfulAuthentication() 호출
  └─ SuccessHandler 호출

  ↓
LocalAuthenticationSuccessHandler
  └─ JSON 응답 (사용자 정보)

응답: JSESSIONID 쿠키 ✓
```

**인증이 필요한 API 호출:**
```
GET /v1/local/check (JSESSIONID 쿠키 포함)
  ↓
Spring Security Filter Chain
  ├─ JSESSIONID 쿠키 감지
  ├─ HttpSessionSecurityContextRepository로 세션 로드
  └─ SecurityContext 복원 ← 이제 가능!

  ↓
LocalAuthController.localLoginCheck()
  ├─ SecurityContextHolder.getContext() 호출
  ├─ Authentication 확인
  └─ 사용자 정보 반환 ✓

응답: 200 OK (사용자 정보) ✓
```

### 17.5 수정된 파일 목록

| 파일 경로 | 수정 사항 |
|---------|---------|
| `config/SecurityConfig.java` | 1. Import 추가 (HttpSessionSecurityContextRepository, SecurityContextRepository)<br/>2. securityContextRepository() 빈 등록<br/>3. securityFilterChain() 메서드에서 명시적으로 설정 |
| `Member_woonkim/infrastructure/auth/filter/JsonUsernamePasswordAuthenticationFilter.java` | 1. Import 추가 (SecurityContext, SecurityContextHolder, HttpSessionSecurityContextRepository)<br/>2. successfulAuthentication() 메서드에서 SecurityContext를 명시적으로 HttpSession에 저장 |

### 17.6 검증 및 테스트

**테스트 흐름:**

```bash
# 1. 애플리케이션 재시작
./gradlew bootRun

# 2. 로그인 API 호출
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}' \
  -c cookies.txt \
  -v

# 응답 헤더에서 JSESSIONID 쿠키 확인:
# Set-Cookie: JSESSIONID=xxxxx; Path=/; HttpOnly

# 3. 인증 상태 확인 (쿠키 포함)
curl -X GET http://localhost:8080/v1/local/check \
  -b cookies.txt \
  -v

# 예상 응답: 200 OK (사용자 정보 반환) ✓
# {
#   "success": true,
#   "data": {
#     "id": 1,
#     "email": "user@example.com",
#     "name": "User Name",
#     ...
#   },
#   ...
# }
```

**로그 확인:**

```
# SecurityConfig에서 SecurityContextRepository 빈 등록 확인
# JsonUsernamePasswordAuthenticationFilter에서 로그 확인:
# "SecurityContext를 세션에 저장 완료 - email: user@example.com"
```

### 17.7 핵심 개선 사항

| 항목 | 이전 | 이후 |
|------|------|------|
| SecurityContextRepository 설정 | ❌ 부재 | ✓ HttpSessionSecurityContextRepository 명시 |
| SecurityContext 저장 | ❌ 위임만 수행 | ✓ 명시적으로 저장 |
| 세션 기반 인증 상태 유지 | ❌ 불가 | ✓ 가능 |
| 후속 API 호출 시 인증 | ❌ UNAUTHORIZED | ✓ 정상 동작 |
| 로그 추적성 | ❌ 불명확 | ✓ 명확한 로그 제공 |

### 17.8 보안 고려사항

✓ **HttpSessionSecurityContextRepository 사용:**
- Spring Security 권장 방식
- 세션 기반 인증에 최적화
- CSRF 보호와 통합

✓ **명시적 SecurityContext 저장:**
- 저장 여부를 명확히 확인 가능
- 디버깅 시 추적 용이
- 향후 Redis로 마이그레이션 시에도 호환

### 17.9 향후 개선 사항 (선택사항)

**프로덕션 환경에서 Redis 저장소로 변경:**

```java
@Bean
public SecurityContextRepository securityContextRepository(RedisOperationsSessionRepository sessionRepository) {
    // Redis 기반 분산 세션 저장소로 변경 가능
    return new HttpSessionSecurityContextRepository();
}
```

현재는 로컬 개발용으로 HttpSession을 사용하며, 프로덕션 배포 시에만 Redis로 변경하면 됩니다.
