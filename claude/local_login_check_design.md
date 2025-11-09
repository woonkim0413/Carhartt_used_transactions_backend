# Local Login Check 엔드포인트 - 설계 문서

**Date:** 2025-11-08
**Requirement:** Local 로그인 사용자의 로그인 상태 확인 API 필요

---

## 📋 요구사항

Local 로그인(이메일/비밀번호)으로 로그인한 사용자의 로그인 상태를 확인할 수 있는 API 필요.

**Endpoint:** `GET /v1/local/check`

---

## 🏗️ 설계

### 1. 참고 사항

OauthController의 `oauthLoginCheck()` 메서드 (라인 265-316)를 참고:
- ✅ `@AuthenticationPrincipal CustomOAuth2User` - Principal에서 사용자 정보 추출
- ✅ null 체크 → `OauthException(OauthErrorCode.C003)` 발생
- ✅ DB에서 Member 조회하여 최신 정보 반환
- ✅ LoginCheckDto로 응답

### 2. Local 로그인의 특성

**OAuth2 로그인:**
- Principal: `CustomOAuth2User`
- SecurityContext에 `OAuth2AuthenticationToken` 저장
- `@AuthenticationPrincipal CustomOAuth2User` 사용

**Local 로그인:**
- Principal: `UserDetails` 구현체 (이메일 문자열로 저장)
- SecurityContext에 `UsernamePasswordAuthenticationToken` 저장
- `@AuthenticationPrincipal` 사용 불가 → `SecurityContextHolder`에서 직접 추출 필요

### 3. 구현 방식

```
클라이언트: GET /v1/local/check
    ↓
LocalAuthController.localLoginCheck()
    ↓
SecurityContextHolder.getContext().getAuthentication() 추출
    ↓
Authentication 검증 (null/비인증 체크)
    ↓
Principal.getName() → email 추출
    ↓
LocalAuthUseCase.getMemberByEmail() → DB에서 Member 조회
    ↓
LoginCheckDto 생성 및 응답
```

### 4. 에러 처리

| 상황 | 에러 코드 | HTTP Status |
|------|----------|------------|
| 로그인하지 않은 사용자 | C003 | 401 Unauthorized |
| 데이터베이스 오류 | M001 | 500 Internal Server Error |

---

## 📝 코드 구조

### LocalAuthController에 추가

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
    // 1. Authentication 추출
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();

    // 2. 로그인 상태 검증
    if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
        throw new LocalAuthException(LocalAuthErrorCode.C003); // 비로그인 상태
    }

    // 3. email 추출 (Local 로그인은 principal이 이메일 문자열)
    String email = auth.getName();

    // 4. DB에서 Member 조회
    Member member = localAuthUseCase.getMemberByEmail(email);

    // 5. LoginCheckDto 생성
    LoginCheckDto dto = LoginCheckDto.builder()
            .memberId(member.getMemberId())
            .memberName(member.getName())
            .memberNickname(member.getNickname())
            .loginType(LoginType.LOCAL.getLoginType())
            .provider(member.getLocalProvider().getProviderName()) // "LOCAL"
            .email(member.getEmail())
            .profileImageUrl(member.getProfileImageUrl())
            .build();

    // 6. 응답
    MetaData meta = CreateMetaData.createMetaData(LocalDateTime.now(), xRequestId);
    return ResponseEntity.ok()
            .header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(ApiResponse.success(dto, meta));
}
```

---

## 🔧 필요한 작업

### 1. LocalAuthUseCase에 메서드 추가
```java
public Member getMemberByEmail(String email) {
    return memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
            .orElseThrow(() -> new LocalAuthException(LocalAuthErrorCode.M001));
}
```

### 2. LocalAuthController에 메서드 추가
```java
@GetMapping("/check")
public ResponseEntity<ApiResponse<LoginCheckDto>> localLoginCheck(...)
```

### 3. 필요한 import 추가
- `SecurityContextHolder`
- `HttpHeaders`
- `CreateMetaData`

---

## 🧪 테스트 시나리오

### Case 1: 로그인한 사용자가 check 요청
```
Authorization: Session (JSESSIONID 쿠키)
GET /v1/local/check
→ 200 OK
{
  "success": true,
  "data": {
    "memberId": 1,
    "memberName": "테스트",
    "memberNickname": "테스트닉네임",
    "loginType": "LOCAL",
    "provider": "LOCAL",
    "email": "test@example.com",
    "profileImageUrl": null
  }
}
```

### Case 2: 로그인하지 않은 사용자가 check 요청
```
GET /v1/local/check
→ 401 Unauthorized
{
  "success": false,
  "error": {
    "code": "C003",
    "message": "로그인 상태가 아닙니다"
  }
}
```

---

## 📊 이전 작업과의 비교

| 항목 | OauthController.oauthLoginCheck | LocalAuthController.localLoginCheck |
|------|--------------------------------|-------------------------------------|
| URL | GET /v1/oauth/login/check | GET /v1/local/check |
| Principal 타입 | CustomOAuth2User | String (email) |
| Authentication 추출 | @AuthenticationPrincipal | SecurityContextHolder |
| provider | attributes.get("provider") | LocalProvider.LOCAL |
| 응답 DTO | LoginCheckDto | LoginCheckDto (동일) |
| 에러 처리 | OauthException | LocalAuthException |

---

## ✅ 체크리스트

- [ ] LocalAuthUseCase.getMemberByEmail() 메서드 추가
- [ ] LocalAuthController.localLoginCheck() 메서드 추가
- [ ] 필요한 import 확인
- [ ] 빌드 성공 확인
- [ ] 테스트 완료
- [ ] CLAUDE.md 업데이트

