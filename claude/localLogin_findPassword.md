# 비밀번호 찾기(재설정) API 설계

## 개요
Local 로그인 사용자의 비밀번호 재설정 기능을 구현합니다. 이메일 인증 코드를 이용한 안전한 비밀번호 재설정 프로세스입니다.

---

## API 엔드포인트 설계

### 1. 비밀번호 찾기 - 인증 코드 전송 API
비밀번호를 찾고자 하는 사용자가 자신의 이메일을 입력하면, 해당 이메일로 인증 코드를 전송합니다.

**Endpoint Path:** `POST /v1/local/password/find`

**Request DTO:**
```java
// PasswordFindRequestDto.java
public record PasswordFindRequestDto(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email
) {}
```

**Response DTO:**
```java
// SuccessMessageResponseDto (기존 재사용)
public class SuccessMessageResponseDto {
    private String message; // "인증 코드가 발송되었습니다"
}
```

**비즈니스 로직:**
- 입력받은 이메일 유효성 검증 (형식, 공백 제거)
- 해당 이메일로 가입된 LOCAL 계정 존재 여부 확인
  - 없으면: `LocalAuthErrorCode.C002` (가입되지 않은 이메일입니다)
- 6자리 난수 코드 생성 (기존 `VerificationCode.generate()` 재사용)
- 인증 코드 저장소에 코드 저장 (TTL: 10분, 기존 `EmailVerificationCodeStore` 재사용)
- 이메일로 인증 코드 전송 (기존 `EmailService` 재사용)

**위치:**
- Controller: `LocalAuthController.java` (line 46-63 의 emailVerification 엔드포인트 패턴 참고)
- UseCase: `PasswordRecoveryUseCase.java` (새로 생성)
  - 메서드: `public SuccessMessageResponseDto sendPasswordResetCode(PasswordFindRequestDto dto)`

**오류 코드:**
- `LocalAuthErrorCode.C002`: 가입되지 않은 이메일입니다
- `EmailErrorCode.E001`: 이메일로 코드 전송 실패했습니다

---

### 2. 비밀번호 찾기 - 인증 코드 검증 및 비밀번호 재설정 API
사용자가 받은 인증 코드와 새로운 비밀번호를 입력하여 비밀번호를 재설정합니다.

**Endpoint Path:** `POST /v1/local/password/reset`

**Request DTO:**
```java
// PasswordResetRequestDto.java
public record PasswordResetRequestDto(
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "유효한 이메일 형식이 아닙니다")
    String email,

    @NotBlank(message = "인증 코드는 필수입니다")
    @Size(min = 6, max = 6, message = "인증 코드는 6자리입니다")
    @Pattern(regexp = "^[0-9]{6}$", message = "인증 코드는 숫자만 포함해야 합니다")
    String code,

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Size(min = 8, max = 50, message = "비밀번호는 8~50자여야 합니다")
    String newPassword
) {}
```

**Response DTO:**
```java
// SuccessMessageResponseDto (기존 재사용)
public class SuccessMessageResponseDto {
    private String message; // "비밀번호가 성공적으로 변경되었습니다"
}
```

**비즈니스 로직:**
- 입력받은 이메일 유효성 검증 (형식, 공백 제거)
- 해당 이메일로 가입된 LOCAL 계정 존재 여부 확인
  - 없으면: `LocalAuthErrorCode.C002` (가입되지 않은 이메일입니다)
- 인증 코드 검증
  - 저장소에서 코드 조회 (없으면: `EmailErrorCode.E002` - 코드 만료)
  - 입력 코드와 저장 코드 비교 (불일치: `EmailErrorCode.E003` - 코드 불일치)
- 새 비밀번호 BCrypt 암호화
- 데이터베이스의 회원 비밀번호 업데이트
- 인증 코드 저장소에서 코드 삭제 (재사용 방지)

**위치:**
- Controller: `LocalAuthController.java`
  - 메서드: `POST /v1/local/password/reset`
- UseCase: `PasswordRecoveryUseCase.java`
  - 메서드: `public SuccessMessageResponseDto resetPassword(PasswordResetRequestDto dto)`

**오류 코드:**
- `LocalAuthErrorCode.C002`: 가입되지 않은 이메일입니다
- `EmailErrorCode.E002`: 인증 코드가 만료되었습니다
- `EmailErrorCode.E003`: 인증 코드가 일치하지 않습니다

---

## 기존 코드 재사용 전략

### 1. 동일한 방식으로 재사용:
- **인증 코드 생성**: `VerificationCode.generate()` (domain/value)
- **코드 저장소**: `EmailVerificationCodeStore` (infrastructure/cache)
- **이메일 전송**: `EmailService.sendVerificationCodeEmail()` (infrastructure/mail)
- **응답 형식**: `SuccessMessageResponseDto` (presentation/dto)

### 2. 다른 저장소 필요 여부:
현재 `EmailVerificationCodeStore`는 회원가입 인증 코드 저장 시에만 사용됩니다.
- **선택 1** (권장): 동일한 저장소를 재사용하되, 나중에 코드 타입을 구분할 수 있도록 확장
- **선택 2**: 비밀번호 재설정용 별도 저장소 `PasswordResetCodeStore` 생성

현재는 **선택 1 (동일 저장소 재사용)**을 권장합니다. 회원가입 단계에서는 인증만 필요하고, 비밀번호 재설정 단계에서는 인증 후 비밀번호만 변경하므로 충돌이 없기 때문입니다.

---

## 컨트롤러 메서드 시그니처

```java
@RestController
@RequestMapping("/v1/local")
public class LocalAuthController {

    private final PasswordRecoveryUseCase passwordRecoveryUseCase;

    /**
     * 비밀번호 찾기 - 인증 코드 전송
     *
     * @param request 이메일 정보
     * @return 성공 메시지
     */
    @PostMapping("/password/find")
    @Operation(summary = "비밀번호 찾기 - 인증 코드 전송",
               description = "입력한 이메일로 비밀번호 재설정 인증 코드를 전송합니다")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> sendPasswordResetCode(
            @Valid @RequestBody PasswordFindRequestDto request
    ) {
        // 구현: passwordRecoveryUseCase.sendPasswordResetCode(request)
    }

    /**
     * 비밀번호 찾기 - 인증 코드 검증 및 비밀번호 재설정
     *
     * @param request 이메일, 인증 코드, 새 비밀번호
     * @return 성공 메시지
     */
    @PostMapping("/password/reset")
    @Operation(summary = "비밀번호 찾기 - 비밀번호 재설정",
               description = "인증 코드를 검증하고 새로운 비밀번호로 변경합니다")
    public ResponseEntity<ApiResponse<SuccessMessageResponseDto>> resetPassword(
            @Valid @RequestBody PasswordResetRequestDto request
    ) {
        // 구현: passwordRecoveryUseCase.resetPassword(request)
    }
}
```

---

## 유스케이스 메서드 시그니처

```java
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
     * @param dto 비밀번호 찾기 요청 (이메일)
     * @return 성공 메시지
     * @throws LocalAuthException 가입되지 않은 이메일
     * @throws EmailException 이메일 전송 실패
     */
    public SuccessMessageResponseDto sendPasswordResetCode(PasswordFindRequestDto dto) {
        // 1. 이메일 유효성 검증
        // 2. LOCAL 계정 존재 여부 확인
        // 3. 6자리 난수 코드 생성
        // 4. 코드 저장소에 저장 (TTL: 10분)
        // 5. 이메일로 코드 전송
        // 6. 성공 메시지 반환
    }

    /**
     * 인증 코드 검증 및 비밀번호 재설정
     *
     * @param dto 비밀번호 재설정 요청 (이메일, 코드, 새 비밀번호)
     * @return 성공 메시지
     * @throws LocalAuthException 가입되지 않은 이메일
     * @throws EmailException 코드 만료, 코드 불일치
     */
    public SuccessMessageResponseDto resetPassword(PasswordResetRequestDto dto) {
        // 1. 이메일 유효성 검증
        // 2. LOCAL 계정 존재 여부 확인
        // 3. 저장소에서 코드 조회 (없으면 E002 예외)
        // 4. 입력 코드와 저장 코드 비교 (불일치 시 E003 예외)
        // 5. 새 비밀번호 BCrypt 암호화
        // 6. 데이터베이스 회원 비밀번호 업데이트
        // 7. 인증 코드 저장소에서 코드 삭제 (TTL 기반으로 자동 삭제되므로 생략 가능)
        // 8. 성공 메시지 반환
    }
}
```

---

## 데이터 흐름도

### 시나리오 1: 비밀번호 찾기 - 인증 코드 전송

```
Client Request (email)
         ↓
[LocalAuthController.sendPasswordResetCode]
         ↓
[PasswordRecoveryUseCase.sendPasswordResetCode]
         ↓
   1. 이메일 검증
   2. 가입 여부 확인 (LOCAL provider)
   3. VerificationCode.generate() → 6자리 코드
   4. EmailVerificationCodeStore.saveCode(email, code) → TTL 10분
   5. EmailService.sendVerificationCodeEmail(email, code)
   6. SuccessMessageResponseDto 반환
         ↓
[ApiResponse<SuccessMessageResponseDto>]
         ↓
Client Response (200 OK + 성공 메시지)
```

### 시나리오 2: 비밀번호 찾기 - 비밀번호 재설정

```
Client Request (email, code, newPassword)
         ↓
[LocalAuthController.resetPassword]
         ↓
[PasswordRecoveryUseCase.resetPassword]
         ↓
   1. 이메일 검증
   2. 가입 여부 확인 (LOCAL provider)
   3. EmailVerificationCodeStore.getCode(email) → 코드 조회
   4. 입력 코드 vs 저장 코드 비교
   5. PasswordEncoder.encode(newPassword)
   6. Member.password = encodedPassword
   7. MemberRepository.save(member)
   8. SuccessMessageResponseDto 반환
         ↓
[ApiResponse<SuccessMessageResponseDto>]
         ↓
Client Response (200 OK + 성공 메시지)
```

---

## 보안 고려사항

1. **인증 코드 TTL**: 10분으로 설정 (회원가입 인증과 동일)
2. **비밀번호 암호화**: BCrypt 10 rounds (기존 설정과 동일)
3. **코드 형식**: 6자리 숫자만 허용 (VerificationCode 클래스로 제한)
4. **이메일 검증**: 정규식으로 형식 검증 (E004 - 유효하지 않은 이메일)
5. **레이트 제한**: 추후 구현 필요 (현재는 미포함)
6. **인증 상태**: 비밀번호 재설정 시 로그인 상태가 아니어야 함 (보안상 이미 로그인된 사용자는 '/check'로 확인 가능)

---

## 에러 처리 매트릭스

| 시나리오 | 에러 코드 | HTTP Status | 메시지 |
|---------|---------|------------|--------|
| 비밀번호 찾기 - 이메일 미입력 | - | 400 Bad Request | DTO 검증 실패 |
| 비밀번호 찾기 - 잘못된 이메일 형식 | - | 400 Bad Request | DTO 검증 실패 |
| 비밀번호 찾기 - 가입되지 않은 이메일 | C002 | 400 Bad Request | 가입되지 않은 이메일입니다 |
| 비밀번호 찾기 - 이메일 전송 실패 | E001 | 500 Internal Server Error | 이메일로 코드 전송 실패했습니다 |
| 비밀번호 재설정 - 코드 만료 | E002 | 400 Bad Request | 인증 코드가 만료되었습니다 |
| 비밀번호 재설정 - 코드 불일치 | E003 | 400 Bad Request | 인증 코드가 일치하지 않습니다 |
| 비밀번호 재설정 - 가입되지 않은 이메일 | C002 | 400 Bad Request | 가입되지 않은 이메일입니다 |

---

## 구현 순서 [완료 - 2025-11-18]

1. **DTO 생성** ✅
   - `PasswordFindRequestDto.java` - 생성 완료
   - `PasswordResetRequestDto.java` - 생성 완료

2. **UseCase 생성** ✅
   - `PasswordRecoveryUseCase.java` - 생성 완료
   - 메서드: `sendPasswordResetCode()`, `resetPassword()` - 구현 완료

3. **Controller 메서드 추가** ✅
   - `LocalAuthController.sendPasswordResetCode()` - POST /v1/local/password/find - 구현 완료
   - `LocalAuthController.resetPassword()` - POST /v1/local/password/reset - 구현 완료

4. **Member Entity 수정** ✅
   - `changePassword(String newEncodedPassword)` 메서드 추가

5. **테스트 및 빌드** ✅
   - 프로젝트 빌드 성공 (Build SUCCESSFUL)
   - 모든 새 코드 컴파일 성공

---

## 기존 코드와의 통합 포인트

| 기존 컴포넌트 | 위치 | 재사용 방식 |
|------------|------|----------|
| `VerificationCode` | `domain/value/` | `VerificationCode.generate()` |
| `EmailVerificationCodeStore` | `infrastructure/cache/` | `saveCode()`, `getCode()`, 제거는 TTL 자동 |
| `EmailService` | `infrastructure/mail/` | `sendVerificationCodeEmail()` |
| `SuccessMessageResponseDto` | `presentation/dto/Local/response/` | 응답 형식 재사용 |
| `LocalAuthErrorCode` | `exception/` | C002 사용 (기존 사용) |
| `EmailErrorCode` | `exception/` | E001, E002, E003 사용 (기존 사용) |
| `MemberRepository` | `infrastructure/db/` | 회원 조회 및 업데이트 |
| `PasswordEncoder` | Spring Security | BCrypt 암호화 |

---

## API 호출 예시

### 1. 인증 코드 전송 요청
```bash
POST /v1/local/password/find
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "인증 코드가 발송되었습니다"
  },
  "error": null,
  "metaData": {
    "timestamp": "2024-11-18T10:30:00",
    "requestId": null
  }
}
```

### 2. 비밀번호 재설정 요청
```bash
POST /v1/local/password/reset
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "123456",
  "newPassword": "NewPassword123!"
}
```

**응답 (200 OK):**
```json
{
  "success": true,
  "data": {
    "message": "비밀번호가 성공적으로 변경되었습니다"
  },
  "error": null,
  "metaData": {
    "timestamp": "2024-11-18T10:35:00",
    "requestId": null
  }
}
```

### 3. 오류 응답 예시
```bash
POST /v1/local/password/reset
Content-Type: application/json

{
  "email": "user@example.com",
  "code": "999999",
  "newPassword": "NewPassword123!"
}
```

**응답 (400 Bad Request):**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "E003",
    "message": "인증 코드가 일치하지 않습니다"
  },
  "metaData": {
    "timestamp": "2024-11-18T10:35:00",
    "requestId": null
  }
}
```
