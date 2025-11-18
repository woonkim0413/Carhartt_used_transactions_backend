# 🔐 JsonUsernamePasswordAuthenticationFilter의 successfulAuthentication() 아키텍처 분석

## 1. 핵심 질문

> **Q: super.successfulAuthentication()을 실행하면 ThreadLocal뿐만 아니라 Session에도 SecurityContext를 저장하지 않아? 왜 명시적으로 저장을 안 한다는거야?**

---

## 2. 답변: super.successfulAuthentication()은 세션에 저장하지 않음

### 2.1 Spring Security 소스 코드 분석

```java
// org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
protected void successfulAuthentication(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain,
                                        Authentication authResult)
        throws IOException, ServletException {

    // Step 1: ThreadLocal에만 저장
    SecurityContextHolder.getContext().setAuthentication(authResult);

    // Step 2: RememberMe 토큰 발급
    rememberMeServices.loginSuccess(request, response, authResult);

    // Step 3: 성공 핸들러 호출
    successHandler.onAuthenticationSuccess(request, response, authResult);

    // 🚨 여기서 끝! 세션 저장 로직이 없음!
}
```

**핵심:**
- `super.successfulAuthentication()`은 **ThreadLocal에만 저장**
- **세션(HttpSession)에는 저장하지 않음**
- RememberMe 토큰, 성공 핸들러만 처리

---

## 3. SecurityContextRepository란 무엇인가?

### 3.1 SecurityContextRepository 인터페이스

```java
// org.springframework.security.web.context.SecurityContextRepository
public interface SecurityContextRepository {

    // SecurityContext를 저장소(세션, 쿠키, etc)에서 로드
    SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder);

    // SecurityContext를 저장소에 저장
    void saveContext(SecurityContext context, HttpServletRequest request,
                     HttpServletResponse response);

    // 저장소에서 SecurityContext 삭제
    boolean containsContext(HttpServletRequest request);
}
```

### 3.2 주요 구현체들

| 구현체 | 저장위치 | 사용시점 |
|------|--------|--------|
| `HttpSessionSecurityContextRepository` | HttpSession (JSESSIONID) | 세션 기반 인증 |
| `RequestAttributeSecurityContextRepository` | 요청 속성 | 같은 요청 내에서만 유효 |
| `DelegatingSecurityContextRepository` | 여러 저장소 조합 | 복합 사용 |
| `NullSecurityContextRepository` | 저장 안 함 | Stateless (JWT 등) |

---

## 4. HttpSessionSecurityContextRepository의 내부 동작

### 4.1 loadContext() - 세션에서 로드

```java
// org.springframework.security.web.context.HttpSessionSecurityContextRepository
@Override
public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
    // 1. 세션에서 SecurityContext 로드
    HttpSession session = requestResponseHolder.getRequest().getSession(false);
    if (session == null) {
        return SecurityContextHolder.createEmptyContext();
    }

    // 2. SPRING_SECURITY_CONTEXT라는 세션 속성에 저장된 객체 조회
    Object contextFromSession = session.getAttribute(
        "SPRING_SECURITY_CONTEXT"
    );

    if (contextFromSession instanceof SecurityContext) {
        return (SecurityContext) contextFromSession;
    }

    return SecurityContextHolder.createEmptyContext();
}
```

### 4.2 saveContext() - 세션에 저장 (핵심!)

```java
@Override
public void saveContext(SecurityContext context,
                       HttpServletRequest request,
                       HttpServletResponse response) {

    // 1. HttpSession 획득 (없으면 생성)
    // ⚠️ 이 과정에서 서블릿이 JSESSIONID 쿠키를 자동 발급!
    HttpSession session = request.getSession();

    // 2. SecurityContext를 세션 속성에 저장
    // 핵심: SPRING_SECURITY_CONTEXT라는 속성명으로 저장
    session.setAttribute("SPRING_SECURITY_CONTEXT", context);

    // 3. 세션 마지막 접근 시간 업데이트
    session.getLastAccessedTime();
}
```

**핵심 메커니즘:**
1. `saveContext()`를 호출하면 `request.getSession()`으로 세션 생성/획득
2. 세션의 `setAttribute("SPRING_SECURITY_CONTEXT", context)` 호출
3. 서블릿 엔진이 자동으로 `JSESSIONID` 쿠키를 HTTP 응답에 추가
4. 브라우저는 `JSESSIONID` 쿠키를 저장했다가 다음 요청에 포함
5. 다음 요청에서 Spring Security는 `loadContext()`로 세션에서 로드

---

## 5. Spring Security 5.x vs 6.x의 차이점

### 5.1 Spring Security 5.x (Deprecated)

```java
// SecurityContextPersistenceFilter (자동으로 FilterChain 감싸기)
public void doFilter(...) {
    SecurityContext context = repo.loadContext(request);  // ← 자동
    SecurityContextHolder.setContext(context);

    try {
        filterChain.doFilter(request, response);
    } finally {
        repo.saveContext(context, request, response);  // ← 자동 (finally 블록)
    }
}
```

**문제점:**
- 명시적 제어 불가능
- 응답이 이미 committed된 경우 saveContext() 실패 가능성

### 5.2 Spring Security 6.x (현재 프로젝트)

```java
// SecurityContextHolderFilter (새로운 구조)
public void doFilter(...) {
    SecurityContext context = repo.loadContext(request);
    SecurityContextHolder.setContext(context);
    filterChain.doFilter(request, response);
    // 자동 cleanup (마지막에 clearContext())
}

// 개발자가 명시적으로 saveContext() 호출
repository.saveContext(context, request, response);
```

**장점:**
- 정확한 타이밍 제어 가능
- 응답 상태 확인 후 저장 여부 결정 가능
- 더 명확한 인증 흐름

---

## 6. 기본 설정에서 SecurityContextRepository가 설정되지 않으면?

### 6.1 기본 동작 (매우 위험!)

Spring Security 6.0 이상에서 명시적 설정이 없으면:

```java
// 기본값: DelegatingSecurityContextRepository 사용
public SecurityContextRepository getSecurityContextRepository() {
    return new DelegatingSecurityContextRepository(
        // Load: 세션 + 요청 속성 모두 시도
        new HttpSessionSecurityContextRepository(),
        new RequestAttributeSecurityContextRepository()
    );
}

// DelegatingSecurityContextRepository의 saveContext():
@Override
public void saveContext(SecurityContext context,
                       HttpServletRequest request,
                       HttpServletResponse response) {
    for (SecurityContextRepository delegate : delegates) {
        // 🚨 RequestAttributeSecurityContextRepository만 saveContext() 구현
        // HttpSessionSecurityContextRepository는 저장하지 않음!
        delegate.saveContext(context, request, response);
    }
}
```

**결과:**
- `RequestAttributeSecurityContextRepository`가 사용됨
- 요청 범위 내에서만 유지됨
- 다음 요청에서는 새로운 SecurityContext 생성
- **로그인 상태가 유지되지 않음!** ❌

### 6.2 현재 프로젝트의 올바른 설정 (SecurityConfig.java)

```java
// 명시적 빈 등록 ✅
@Bean
public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
}

// securityFilterChain에서 명시적 설정 ✅
http.securityContext(securityContext ->
    securityContext.securityContextRepository(securityContextRepository())
);
```

---

## 7. 응답(Response) 상태의 중요성

### 7.1 ServletResponse "committed" 상태란?

```
응답 상태 전환:
┌─────────────┐
│  NOT SENT   │  초기 상태
└──────┬──────┘
       │ response.getWriter() / getOutputStream() 첫 호출
       ↓
┌─────────────┐
│ COMMITTED   │  이제부터 HTTP 헤더/상태 코드 변경 불가능
└──────┬──────┘
       │ 응답 완료
       ↓
┌─────────────┐
│   SENT      │  클라이언트에게 전송됨
└─────────────┘
```

### 7.2 committed 상태에서는?

- `response.setStatus()` - 무시됨 (이미 HTTP 200으로 결정됨)
- `response.setHeader()` - 무시됨
- `response.addCookie()` - 무시됨 (**JSESSIONID 쿠키 설정 불가**)

### 7.3 response.isCommitted() 메서드

```java
boolean isCommitted = response.isCommitted();
```

- `true` = 응답 바디가 이미 클라이언트로 전송되기 시작함
- `false` = 아직 응답을 조작할 수 있음 (헤더 추가/수정 가능)

---

## 8. 두 번째 핵심 질문: SuccessHandler와 response.getWriter()

> **Q: SuccessHandler에서 response.getWriter().write()를 호출하지 않으면 응답 필터(또는 핸들러)에서 SecurityContextRepository.saveContext()를 정상적으로 호출할까?**

### 8.1 답변: **YES, 정상적으로 호출된다**

다만 **호출 순서가 매우 중요합니다.**

---

## 9. 정확한 호출 순서 (다이어그램)

### 9.1 현재 프로젝트의 구현 (올바른 순서)

```
요청 시작
    ↓
[SecurityContextHolderFilter]
    ├─ loadContext() 호출
    │  └─ HttpSession 로드 (JSESSIONID 쿠키 검사)
    │  └─ SecurityContext 반환 (세션에서 조회)
    ├─ SecurityContextHolder.setContext(context)
    │
    └─ FilterChain 계속
         ↓
[JsonUsernamePasswordAuthenticationFilter]
    ├─ attemptAuthentication()
    │  └─ AuthenticationManager.authenticate()
    │     └─ LocalUserDetailsService.loadUserByUsername()
    │     └─ PasswordEncoder.matches() ✅ 비밀번호 일치
    │
    └─ successfulAuthentication() ← 인증 성공
         ├─ SecurityContext context = createEmptyContext()
         ├─ context.setAuthentication(authResult)
         ├─ SecurityContextHolder.setContext(context) [ThreadLocal]
         │
         ├─ ⏰ T1: HttpSessionSecurityContextRepository.saveContext() ← CRITICAL
         │  ├─ request.getSession(true)  ← 세션 생성 (response not committed ✅)
         │  ├─ session.setAttribute("SPRING_SECURITY_CONTEXT", context) ← 저장
         │  └─ JSESSIONID 쿠키 HTTP 헤더에 추가됨 ✅
         │
         └─ super.successfulAuthentication() ← 부모 메서드
              └─ [UsernamePasswordAuthenticationFilter의 기본 동작]
                  └─ ⏰ T2: localSuccessHandler.onAuthenticationSuccess()
                      ├─ request.getSession(true)  ← 기존 세션 재사용
                      ├─ memberRepository.findByEmail()
                      ├─ response.setContentType(...)  [OK - not committed yet]
                      ├─ response.setStatus(...)       [OK - not committed yet]
                      └─ response.getWriter().write(...) ← response 이제 COMMITTED
                           └─ HTTP 응답 바디 작성
                               └─ 바디 flush (헤더는 이미 T1에서 결정됨)
                                   └─ JSESSIONID 쿠키 포함되어 전송됨 ✅

응답 완료 (JSESSIONID 쿠키 + JSON 응답 바디)
    ↓
[클라이언트]
    ├─ JSESSIONID 쿠키 저장
    │
    └─ 다음 요청 시 JSESSIONID 쿠키 자동 포함

[다음 요청]
    ↓
[SecurityContextHolderFilter]
    └─ loadContext()
       └─ JSESSIONID 쿠키 자동 송신
       └─ HttpSession 조회
       └─ session.getAttribute("SPRING_SECURITY_CONTEXT") 로드
       └─ SecurityContextHolder.setContext() ← 사용자 인증 상태 복원 ✅
```

### 9.2 호출 순서의 중요성

```
✅ 올바른 순서: saveContext() → getWriter().write()
   └─ T1 시점: response.isCommitted() = false
      ├─ JSESSIONID 쿠키 설정됨 ✅
      └─ SecurityContext 저장됨 ✅

   └─ T2 시점: response.isCommitted() = true
      └─ 응답 바디 작성 (헤더는 이미 확정)
      └─ JSESSIONID 쿠키 포함되어 전송됨 ✅

❌ 잘못된 순서: getWriter().write() → saveContext()
   └─ T1 시점: response.getWriter().write()
      └─ response.isCommitted() = true로 변경

   └─ T2 시점: saveContext() 호출
      └─ request.getSession()에서 쿠키 설정 시도
      └─ response는 이미 committed → 쿠키 설정 실패 ❌
      └─ 다음 요청에서 세션을 찾을 수 없음 ❌
      └─ 로그인 상태가 유지되지 않음 ❌
```

---

## 10. 현재 프로젝트 코드 분석

### 10.1 JsonUsernamePasswordAuthenticationFilter.java (라인 161-189)

```java
@Override
protected void successfulAuthentication(HttpServletRequest request,
                                        HttpServletResponse response,
                                        FilterChain chain,
                                        Authentication authResult) throws IOException, ServletException {

    // Step 1: SecurityContext 생성
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authResult);

    // Step 2: ThreadLocal에 저장 (현재 스레드용)
    SecurityContextHolder.setContext(context);

    // Step 3: 세션에 명시적으로 저장 ← 핵심!
    SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
    repository.saveContext(context, request, response);

    log.info("JsonUsernamePasswordAuthenticationFilter.successfulAuthentication: "
            + "SecurityContext를 세션에 저장 완료 - email: {}", authResult.getName());

    // Step 4: 부모 클래스의 표준 처리 실행 (SuccessHandler 호출)
    super.successfulAuthentication(request, response, chain, authResult);

    log.info("JsonUsernamePasswordAuthenticationFilter.successfulAuthentication: 인증 성공 - email: {}",
            authResult.getName());
    LogPaint.sep("login 처리 이탈");
}
```

### 10.2 LocalAuthenticationSuccessHandler.java (라인 85-95)

```java
@Override
public void onAuthenticationSuccess(HttpServletRequest request,
                                    HttpServletResponse response,
                                    Authentication authentication) throws IOException, ServletException {

    String email = authentication.getName();
    Member member = memberRepository.findByLocalProviderAndEmail(LocalProvider.LOCAL, email)
            .orElseThrow(() -> new LocalAuthException(LocalAuthErrorCode.M003));

    ApiResponse<LoginResponseDto> apiResponse = ApiResponse.success(LoginResponseDto.from(member));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));  // ← 응답 바디 작성
}
```

### 10.3 분석 결과

| 항목 | 현재 코드 |
|------|---------|
| **saveContext() 호출** | ✅ 있음 (라인 175-176) |
| **호출 위치** | ✅ super.successfulAuthentication() **이전** |
| **response 상태** | ✅ not committed (getWriter() 호출 전) |
| **JSESSIONID 쿠키** | ✅ 정상 설정됨 |
| **SecurityContext 저장** | ✅ HttpSession에 정상 저장됨 |
| **세션 기반 인증** | ✅ 다음 요청에서 정상 작동 |

---

## 11. 왜 명시적으로 저장해야 하는가?

### 11.1 문제 상황

당신이 명시적 `repository.saveContext()`를 호출하지 않았다면:

```
로그인 요청 (POST /v1/local/login)
  ↓
JsonUsernamePasswordAuthenticationFilter.successfulAuthentication()
  ├─ SecurityContext를 ThreadLocal에만 저장 (super() 호출)
  └─ 세션에는 저장하지 않음 ❌
  ↓
응답 반환 (JSESSIONID 쿠키 발급... 되지 않음?)
  ↓
ThreadLocal 정리 (필터 체인 종료)
  └─ SecurityContext는 메모리에서 제거됨
  ↓
다음 요청 (GET /v1/local/check, JSESSIONID 쿠키는 없음)
  ├─ 세션에 SecurityContext 없음 (저장되지 않았음)
  └─ C003 에러 발생: "로그인 상태가 아닙니다" ❌
```

### 11.2 해결책 (현재 코드)

```
로그인 요청 (POST /v1/local/login)
  ↓
JsonUsernamePasswordAuthenticationFilter.successfulAuthentication()
  ├─ SecurityContext 생성
  ├─ ThreadLocal에 저장
  ├─ 명시적으로 saveContext() 호출 ✅
  │  └─ HttpSession 생성
  │  └─ JSESSIONID 쿠키 설정됨 ✅
  │  └─ session.setAttribute("SPRING_SECURITY_CONTEXT", context) 저장됨 ✅
  └─ super() 호출 (RememberMe, SuccessHandler)
  ↓
응답 반환 (JSESSIONID 쿠키 포함)
  ↓
ThreadLocal 정리 (관계없음 - 세션에 저장되어 있음)
  ↓
다음 요청 (GET /v1/local/check, JSESSIONID 쿠키 포함)
  ├─ 세션 조회
  └─ session.getAttribute("SPRING_SECURITY_CONTEXT") 로드 ✅
     └─ 사용자 인증 상태 복원 ✅
        └─ 정상 응답 (200 OK) ✅
```

---

## 12. 응답 상태별 동작 정리

| 시점 | response.isCommitted() | 가능한 작업 | 비고 |
|------|----------------------|-----------|------|
| saveContext() 호출 시 | **false** | HTTP 헤더 추가 (쿠키 설정) | ✅ JSESSIONID 정상 설정 |
| getWriter().write() 호출 후 | **true** | 바디만 작성 가능 | 헤더는 이미 전송됨 |
| flush() 이후 | **true** | 아무것도 불가능 | 응답 완전 종료 |

---

## 13. 최종 결론

### 13.1 핵심 답변

| 질문 | 답변 | 이유 |
|------|------|------|
| **super.successfulAuthentication()이 세션에 저장하나?** | ❌ NO | 부모 메서드는 ThreadLocal에만 저장, 세션 저장 로직 없음 |
| **왜 명시적으로 저장을 안 한다고?** | Spring Security의 기본 설계 때문 | super()는 RememberMe, SuccessHandler만 담당, 세션 저장은 SecurityContextRepository의 책임 |
| **SuccessHandler에서 write() 호출하면 saveContext() 작동?** | ✅ YES | saveContext()가 먼저 호출되므로 JSESSIONID 쿠키 정상 설정 |
| **saveContext()와 write() 순서가 반대면?** | ❌ FAIL | response가 committed되어 쿠키 설정 불가능 |

### 13.2 현재 프로젝트 구현 평가

| 항목 | 평가 |
|------|------|
| **SecurityContextRepository 설정** | ✅ 완벽 |
| **http.securityContext() 명시적 설정** | ✅ 완벽 |
| **saveContext() 호출 위치** | ✅ 정확 (super() 이전) |
| **response 상태 관리** | ✅ 정확 (not committed 상태에서 호출) |
| **세션 기반 인증 흐름** | ✅ 정확 |
| **다음 요청의 SecurityContext 복구** | ✅ 정확 |

**결론: 당신의 구현이 정확하고 최적화되어 있습니다!** 🎉

---

## 14. 추가 최적화 (선택사항)

응답 상태를 명시적으로 확인하고 싶다면:

```java
@Override
protected void successfulAuthentication(...) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authResult);
    SecurityContextHolder.setContext(context);

    // ⚠️ 응답 상태 확인 후 저장
    if (!response.isCommitted()) {
        SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
        repository.saveContext(context, request, response);
        log.info("SecurityContext saved to session - email: {}", authResult.getName());
    } else {
        log.error("Response already committed, cannot save SecurityContext");
    }

    super.successfulAuthentication(request, response, chain, authResult);
}
```

---

## 15. response.getWriter().write()와 Spring 자동 저장의 충돌 (2025-11-14)

### 15.1 핵심 질문

> **Q: `super.successfulAuthentication()`만 호출하면 Spring이 자동으로 SecurityContext를 session에 저장하는데, `response.getWriter().write()`를 사용하면 왜 실패하는가?**

### 15.2 답변: 본질적 충돌 없음 (실제로는 작동함!)

**당신이 원하는 구조:**
```java
// JsonUsernamePasswordAuthenticationFilter
super.successfulAuthentication(request, response, chain, authResult);
// ← Spring이 자동으로 session 저장

// LocalAuthenticationSuccessHandler
response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
// ← JSON 응답 반환
```

**이것이 충돌한다고 생각되는 이유:**

많은 개발자들이 다음과 같이 잘못 이해합니다:

```
response.getWriter().write() 호출
  → response COMMITTED
  → 이후의 모든 헤더 설정 불가능 (쿠키 포함)
  → 세션 저장 실패 ❌
```

**하지만 실제 Spring의 동작:**

`super.successfulAuthentication()` 내부의 **정확한 호출 순서** (Spring Security 소스코드):

```java
// org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter
protected void successfulAuthentication(...) {

    // Step 1: SecurityContext 생성
    SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
    context.setAuthentication(authResult);

    // ⏰ T1: SecurityContextRepository에 저장 (JSESSIONID 쿠키 설정)
    this.securityContextRepository.saveContext(context, request, response);
    // ← 이 시점: response가 아직 COMMITTED 아님 ✅
    // ← JSESSIONID 쿠키를 HTTP 헤더에 추가 ✅
    // ← HTTP 헤더 확정 (클라이언트로 전송 예정)

    // Step 2: RememberMe 서비스
    this.rememberMeServices.loginSuccess(request, response, authResult);

    // Step 3: 이벤트 발행
    if (this.eventPublisher != null) {
        this.eventPublisher.publishEvent(...);
    }

    // ⏰ T2: SuccessHandler 호출 ← 당신의 JSON response.getWriter().write() 호출
    this.successHandler.onAuthenticationSuccess(request, response, authResult);
    // ← 여기서 getWriter().write() 호출
    // ← response가 COMMITTED됨
    // ← 하지만 HTTP 헤더는 이미 T1에서 결정됨! (쿠키 포함) ✅
}
```

### 15.3 response Committed 상태의 의미

```
응답 상태 전환:
┌──────────────────┐
│  NOT COMMITTED   │  초기 상태
└────────┬─────────┘
         │ response.getWriter().write() 호출
         │ + 버퍼 꽉 참 또는 flush()
         ↓
┌──────────────────┐
│   COMMITTED      │  HTTP 헤더 & 상태 코드 전송됨 (복구 불가능)
└────────┬─────────┘
         │ 응답 완료
         ↓
┌──────────────────┐
│     SENT         │  클라이언트 수신 완료
└──────────────────┘
```

**Committed 상태의 특징:**
- HTTP 상태 코드 변경 불가능
- HTTP 헤더 추가 불가능
- **JSESSIONID 쿠키 설정 불가능**

**⏰ 타이밍 차이:**
- `T1 (saveContext)`: response NOT COMMITTED → 쿠키 설정 ✅
- `T2 (successHandler)`: response COMMITTED → 헤더는 이미 T1에서 전송됨 ✅

### 15.4 현재 프로젝트의 실제 동작

**Spring의 saveContext() 호출 순서:**
```
T1: super.successfulAuthentication() 시작
    ↓
  saveContext() 실행
    ├─ request.getSession(true) → HttpSession 생성
    ├─ session.setAttribute("SPRING_SECURITY_CONTEXT", context)
    ├─ JSESSIONID 쿠키를 response에 추가 ✅
    ├─ HTTP 헤더 결정 (클라이언트로 전송 대기)
    └─ response.isCommitted() = false ✅

T2: successHandler.onAuthenticationSuccess() 호출
    ├─ response.getWriter().write(json)
    ├─ response가 COMMITTED
    ├─ HTTP 헤더 전송 (JSESSIONID 쿠키 포함) ✅
    └─ 응답 바디 작성
```

**결과: 실제로는 정상 작동함!** ← 당신의 구현이 올바른 이유

### 15.5 왜 명시적 호출이 권장되는가?

**현재 프로젝트의 코드:**
```java
// JsonUsernamePasswordAuthenticationFilter.java
@Override
protected void successfulAuthentication(...) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authResult);
    SecurityContextHolder.setContext(context);

    // 🔧 명시적 호출
    SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
    repository.saveContext(context, request, response);

    log.info("SecurityContext를 세션에 저장 완료 - email: {}", authResult.getName());

    super.successfulAuthentication(request, response, chain, authResult);
}
```

**명시적 호출의 이점:**

| 항목 | 명시적 호출 | Spring 자동 호출 |
|------|----------|-----------------|
| **코드 명확성** | 저장이 명시적 (의도 분명) | 암묵적 (의도 불명확) |
| **디버깅** | 로그 추가 쉬움 | 추적 어려움 |
| **제어** | 저장 시점 명시 | Spring 내부에 의존 |
| **Spring 버전 호환성** | Spring 6.x 권장 방식 | Spring 5.x 방식 (deprecated) |
| **응답 상태 확인** | 필요시 추가 가능 | 불가능 |

### 15.6 Spring Security 5.x vs 6.x 철학 변화

**Spring Security 5.x (Deprecated):**
```java
// SecurityContextPersistenceFilter
public void doFilter(...) {
    SecurityContext context = repo.loadContext(request);
    SecurityContextHolder.setContext(context);

    try {
        filterChain.doFilter(request, response);
    } finally {
        // 자동으로 saveContext() 호출 (명시적 제어 불가)
        repo.saveContext(context, request, response);
    }
}
```

**문제:**
- finally 블록에서 무조건 호출
- 응답이 이미 committed되어 쿠키 설정 실패 가능성
- 명시적 제어 불가능

**Spring Security 6.x (현재):**
```java
// SecurityContextHolderFilter
public void doFilter(...) {
    SecurityContext context = repo.loadContext(request);
    SecurityContextHolder.setContext(context);

    try {
        filterChain.doFilter(request, response);
    } finally {
        SecurityContextHolder.clearContext();  // ← 저장 안 함
    }
}

// 개발자가 명시적으로 호출
repository.saveContext(context, request, response);
```

**Spring Security 6.x의 철학:**
- "명시적이고 명확한 코드가 암묵적인 코드보다 낫다" (PEP 20)
- 응답 상태를 확인한 후 저장 여부 결정 가능
- 성능 최적화 (불필요한 저장 방지)

### 15.7 결론

#### **당신의 질문에 대한 최종 답변**

> "response.getWriter().write()를 사용하면 이후에 ThreadLocal에 담긴 SecurityContext가 session에 정상 저장되지 못한다?"

**답: 아닙니다. Spring의 구조상 정상 저장됩니다.**

**이유:**
1. `super.successfulAuthentication()` 내부에서 `saveContext()`가 **먼저** 실행됨
2. `saveContext()`는 JSESSIONID 쿠키를 HTTP 헤더에 추가함
3. HTTP 헤더는 이 시점에 결정됨 (클라이언트로 전송 준비)
4. 이후 `successHandler.onAuthenticationSuccess()`에서 `getWriter().write()` 호출
5. 응답 바디 작성 시에도 헤더는 이미 결정되었으므로 쿠키 포함됨

#### **현재 구현이 올바른 이유**

```java
// ✅ 올바른 순서 (현재 프로젝트)
SecurityContextRepository repository = new HttpSessionSecurityContextRepository();
repository.saveContext(context, request, response);  // ← T1: 헤더 설정

super.successfulAuthentication(request, response, chain, authResult);
// super() 내부:
//   - rememberMeServices 호출
//   - successHandler 호출
//     └─ response.getWriter().write(json);  // ← T2: 바디 작성
```

헤더가 먼저 결정되므로 바디를 나중에 작성해도 문제없음.

#### **최종 권장 개선 (선택사항)**

```java
@Component
public class JsonUsernamePasswordAuthenticationFilter
    extends UsernamePasswordAuthenticationFilter {

    // 🔧 생성자 주입으로 중복 인스턴스 생성 방지
    private final SecurityContextRepository securityContextRepository;

    public JsonUsernamePasswordAuthenticationFilter(
        SecurityContextRepository securityContextRepository) {
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected void successfulAuthentication(...) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authResult);
        SecurityContextHolder.setContext(context);

        // 🔧 주입된 인스턴스 사용 (매번 생성 안 함)
        securityContextRepository.saveContext(context, request, response);

        log.info("SecurityContext saved to session - email: {}", authResult.getName());

        super.successfulAuthentication(request, response, chain, authResult);
    }
}
```

---

## 참고 자료

- **Spring Security 공식 문서:** https://spring.io/projects/spring-security
- **Spring Security 6.0 마이그레이션:** SecurityContextPersistenceFilter → SecurityContextHolderFilter
- **Servlet API - Response Committed State:** HttpServletResponse.isCommitted()
- **HttpSession - SPRING_SECURITY_CONTEXT:** Spring Security의 세션 저장 속성명
- **Spring Security 6.x Session Management:** https://docs.spring.io/spring-security/reference/servlet/authentication/persistence.html
