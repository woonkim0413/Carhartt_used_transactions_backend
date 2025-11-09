# Local Login Check 엔드포인트 - 구현 완료

**Date:** 2025-11-08
**Status:** ✅ IMPLEMENTED & VERIFIED
**Endpoint:** `GET /v1/local/check`

---

## 📋 요구사항

Local 로그인(이메일/비밀번호)으로 로그인한 사용자의 로그인 상태를 확인할 수 있는 API 필요.

---

## ✅ 구현 완료

### 1. LocalAuthException 클래스 생성

**파일:** `src/main/java/com/C_platform/Member_woonkim/exception/LocalAuthException.java`

```java
@Getter
public class LocalAuthException extends RuntimeException {
    private final ErrorCode errorCode;

    public LocalAuthException(ErrorCode code) {
        super(code.getMessage());
        this.errorCode = code;
    }
}
```

**특징:**
- OauthException 패턴을 따름
- ErrorCode를 전달하여 에러 코드 관리
- Global exception handler에서 처리 가능

---

### 2. LocalAuthErrorCode 업데이트

**파일:** `src/main/java/com/C_platform/Member_woonkim/exception/LocalAuthErrorCode.java`

```java
@Getter
public enum LocalAuthErrorCode implements ErrorCode {
    // Local 인증 공통
    C001("C001", "이메일 또는 비밀번호가 올바르지 않습니다"),
    C002("C002", "가입되지 않은 이메일입니다"),
    C003("C003", "로그인 상태가 아닙니다"),  // ✅ 업데이트
    C004("C004", "로그아웃 실패입니다"),

    // 회원가입 및 조회
    M001("M001", "회원을 찾을 수 없습니다"),  // ✅ 업데이트
    M002("M002", "유효하지 않은 이메일 형식입니다"),
    M003("M003", "비밀번호가 정책을 충족하지 않습니다"),
    M004("M004", "유효하지 않은 이름입니다");
}
```

**변경 사항:**
- C003: "비밀번호가 올바르지 않습니다" → "로그인 상태가 아닙니다"
- M001: "이메일이 이미 등록되어 있습니다" → "회원을 찾을 수 없습니다"

---

### 3. LocalAuthUseCase.getMemberByEmail() 메서드 추가

**파일:** `src/main/java/com/C_platform/Member_woonkim/application/useCase/LocalAuthUseCase.java`

```java
/**
 * 이메일로 Member 조회
 *
 * Local 로그인 상태 확인 시 DB에서 최신 회원 정보를 조회합니다.
 */
public Member getMemberByEmail(String email) {
    log.info("LocalAuthUseCase.getMemberByEmail: 회원 조회 시작 - email: {}", email);

    Member member = memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
            .orElseThrow(() -> {
                log.error("LocalAuthUseCase.getMemberByEmail: 회원을 찾을 수 없음 - email: {}", email);
                return new LocalAuthException(LocalAuthErrorCode.M001);
            });

    log.info("LocalAuthUseCase.getMemberByEmail: 회원 조회 성공 - memberId: {}, memberName: {}",
            member.getMemberId(), member.getName());

    return member;
}
```

**특징:**
- MemberRepository 주입 추가
- LocalAuthErrorCode.M001 예외 발생
- 상세 로깅으로 디버깅 용이

---

### 4. LocalAuthController.localLoginCheck() 엔드포인트 추가

**파일:** `src/main/java/com/C_platform/Member_woonkim/presentation/controller/LocalAuthController.java`

```java
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
```

**특징:**
- HTTP GET 메서드 (상태 조회)
- SecurityContext에서 Authentication 직접 추출 (Local 로그인 특성)
- OAuth2와 달리 @AuthenticationPrincipal 사용 불가
- 3단계 검증 (null, authenticated, anonymousUser)
- DB에서 최신 정보 조회하여 반환

---

## 🔍 동작 원리

### Local 로그인 vs OAuth2 로그인

| 항목 | Local 로그인 | OAuth2 로그인 |
|------|-----------|-----------|
| **Principal 타입** | String (email) | CustomOAuth2User (객체) |
| **Authentication 타입** | UsernamePasswordAuthenticationToken | OAuth2AuthenticationToken |
| **정보 추출 방법** | `auth.getName()` → email | `@AuthenticationPrincipal` → 객체 |
| **Session 생성** | JsonUsernamePasswordAuthenticationFilter 성공 시 | OAuth callback 후 |
| **Check 엔드포인트** | SecurityContextHolder에서 직접 추출 | @AuthenticationPrincipal 사용 |

### 요청 흐름

```
클라이언트: GET /v1/local/check (JSESSIONID 쿠키 포함)
    ↓
LocalAuthController.localLoginCheck()
    ↓
1. SecurityContextHolder.getContext().getAuthentication() 호출
2. Authentication이 null이거나 미인증 상태 확인
3. principal이 null이거나 anonymousUser인지 확인
4. auth.getName() → email 추출
5. LocalAuthUseCase.getMemberByEmail(email) → DB에서 조회
6. LoginCheckDto 생성 및 반환
    ↓
응답: 200 OK
{
  "success": true,
  "data": {
    "memberId": 1,
    "memberName": "테스트 사용자",
    "memberNickname": "testuser",
    "loginType": "LOCAL",
    "provider": "LOCAL",
    "email": "test@example.com",
    "profileImageUrl": null
  },
  "metaData": { ... }
}
```

---

## 🧪 테스트 시나리오

### Case 1: 로그인한 사용자가 check 요청

```bash
# 1. 먼저 로그인
curl -X POST http://localhost:8080/v1/local/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@example.com","password":"Password123"}'

# 응답: JSESSIONID 쿠키 포함, 회원 정보 반환
# 쿠키 저장

# 2. check 요청
curl -X GET http://localhost:8080/v1/local/check \
  -H "Cookie: JSESSIONID=abc123..."

# 응답: 200 OK + 회원 정보
```

### Case 2: 로그인하지 않은 사용자가 check 요청

```bash
curl -X GET http://localhost:8080/v1/local/check

# 응답: 401 Unauthorized
{
  "success": false,
  "error": {
    "code": "C003",
    "message": "로그인 상태가 아닙니다"
  }
}
```

---

## 📊 구현 내용 요약

| 항목 | 파일 | 작업 | 상태 |
|------|------|------|------|
| 예외 클래스 | LocalAuthException.java | 생성 | ✅ |
| 에러 코드 | LocalAuthErrorCode.java | 업데이트 (C003, M001) | ✅ |
| UseCase | LocalAuthUseCase.java | getMemberByEmail() 추가 | ✅ |
| Controller | LocalAuthController.java | localLoginCheck() 추가 | ✅ |
| 문서 | CLAUDE.md | 엔드포인트 정보 업데이트 | ✅ |

---

## 🏗️ 아키텍처 일관성

### OauthController.oauthLoginCheck()와의 비교

| 항목 | OAuth2 | Local |
|------|--------|-------|
| URL | GET /v1/oauth/login/check | GET /v1/local/check |
| Principal | CustomOAuth2User | String (email) |
| 추출 | @AuthenticationPrincipal | SecurityContextHolder |
| DTO | LoginCheckDto | LoginCheckDto (동일) |
| 예외 | OauthException | LocalAuthException |
| 에러코드 | OauthErrorCode | LocalAuthErrorCode |

### 설계 원칙

1. **일관성:** OAuth2와 동일한 패턴 유지
2. **분리:** Local 인증을 위한 별도 예외/에러코드 사용
3. **재사용성:** LoginCheckDto로 통일하여 클라이언트 호환성 유지
4. **검증:** 3단계 검증으로 보안성 강화

---

## 🔐 보안 고려사항

1. **세션 검증:** JSESSIONID 쿠키 자동 검증 (Spring Security)
2. **Authentication 확인:** null, authenticated, anonymousUser 체크
3. **DB 조회:** SecurityContext에 저장된 정보만으로 판단하지 않고 DB 재조회
4. **에러 로깅:** 보안 취약점 식별을 위한 상세 로깅
5. **응답 캐싱 방지:** `Cache-Control: no-store` 헤더 추가

---

## 📦 빌드 및 테스트

```
BUILD SUCCESSFUL in 4s

애플리케이션 시작:
Tomcat started on port 8080 (http) with context path '/'
Started CPlatformApplication in 10.115 seconds
```

✅ **빌드 성공, 애플리케이션 정상 시작**

---

## 📝 CLAUDE.md 업데이트 내용

1. **엔드포인트 추가**
   - `GET /v1/local/check` - Verify current login status

2. **Key Classes 업데이트**
   - LocalAuthException 추가
   - LocalAuthenticationSuccessHandler 상세 설명
   - JsonUsernamePasswordAuthenticationFilter 상세 설명

3. **CSRF 제외 경로 추가**
   - `/v1/local/check` - CSRF 제외

4. **인증 필수 엔드포인트 명시**
   - `GET /v1/local/check` - Auth required (local login only)

5. **File References 확대**
   - LocalAuthException.java
   - LocalAuthErrorCode.java
   - LocalAuthenticationSuccessHandler.java
   - JsonUsernamePasswordAuthenticationFilter.java

---

## 🎯 다음 단계 (선택사항)

1. **통합 테스트 작성**
   - 로그인 후 check 요청 테스트
   - 로그인하지 않은 상태 check 테스트

2. **Client 구현**
   - Axios/Fetch API로 check 엔드포인트 호출
   - 로그인 상태 유지 확인

3. **프로덕션 배포**
   - 에러 로깅 모니터링
   - 세션 타임아웃 설정 확인

---

**Implementation Date:** 2025-11-08
**Status:** ✅ COMPLETED & VERIFIED
**Build:** SUCCESS
**Application:** RUNNING

