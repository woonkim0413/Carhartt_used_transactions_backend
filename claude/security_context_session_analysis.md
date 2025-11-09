# SecurityContext 세션 저장 문제 분석

**Date:** 2025-11-08
**Issue:** SecurityContext가 세션에 제대로 저장되지 않음

---

## 🔍 문제 분석

### Spring Security 인증 흐름

```
클라이언트 요청: POST /v1/local/login
    ↓
JsonUsernamePasswordAuthenticationFilter.attemptAuthentication()
    ├─ JSON 파싱 (email, password)
    ├─ 검증 및 정제
    ├─ UsernamePasswordAuthenticationToken 생성
    └─ AuthenticationManager.authenticate() 호출
    ↓
❌ 기본 동작 (NOT HAPPENING):
    successfulAuthentication() 호출 (부모 클래스의 메서드)
    ├─ SecurityContext 생성
    ├─ Authentication 저장
    ├─ request.getSession(true) 호출 → JSESSIONID 생성
    └─ LocalAuthenticationSuccessHandler.onAuthenticationSuccess() 호출

❌ 실제 동작 (PROBLEM):
    LocalAuthenticationSuccessHandler.onAuthenticationSuccess() 호출
    ├─ SecurityContext 저장 (이미 LocalAuthenticationSuccessHandler에서 함)
    ├─ request.getSession(true) 호출 (이미 LocalAuthenticationSuccessHandler에서 함)
    └─ 그러나 securityContext가 세션에 저장되지 않음!

왜? → HttpSessionSecurityContextRepository를 사용하지 않았기 때문!
```

---

## 🚨 근본 원인

### 1. SecurityContext 저장만으로는 부족함

```java
// LocalAuthenticationSuccessHandler의 현재 코드
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);  // ✅ SecurityContext 저장 (메모리만)

// ❌ 문제: SecurityContext가 세션에 저장되지 않음!
// SecurityContext는 ThreadLocal에 저장되기만 함
// 새로운 요청이 들어올 때 다시 불러올 방법이 없음
```

### 2. HttpSessionSecurityContextRepository 없음

```java
// 필요한 코드 (현재 없음):
HttpSessionSecurityContextRepository repository =
    new HttpSessionSecurityContextRepository();
repository.saveContext(context, request, response);  // ✅ 세션에 저장
```

### 3. 전체 흐름 문제

```
UsernamePasswordAuthenticationFilter (부모 클래스)
├─ attemptAuthentication()
│  └─ AuthenticationManager.authenticate() → 성공하면 Authentication 반환
├─ successfulAuthentication() ← 이 메서드가 핵심!
│  ├─ SecurityContext 생성
│  ├─ Authentication 저장
│  ├─ 세션에 저장 ← HttpSessionSecurityContextRepository 사용
│  └─ SuccessHandler 호출
└─ SuccessHandler.onAuthenticationSuccess()

현재 코드의 문제:
- ✅ attemptAuthentication() - 정상 (필터에서 구현)
- ✅ onAuthenticationSuccess() - 정상 (핸들러에서 구현)
- ❌ successfulAuthentication() - 호출되지 않음!
  (필터를 addFilterAt()으로 추가했기 때문에,
   부모 클래스의 successfulAuthentication() 메서드가 자동 호출되지 않음)
```

---

## 📋 해결 방법

### 방법 1: successfulAuthentication() 메서드 오버라이드 (권장)

```java
@Override
protected void successfulAuthentication(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain chain,
                                       Authentication authResult) throws IOException, ServletException {
    log.info("JsonUsernamePasswordAuthenticationFilter.successfulAuthentication(): 인증 성공");

    // 1. SecurityContext 생성
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authResult);
    SecurityContextHolder.setContext(context);
    log.debug("SecurityContext에 Authentication 저장 완료");

    // 2. ✅ HttpSessionSecurityContextRepository를 사용하여 세션에 저장
    HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    securityContextRepository.saveContext(context, request, response);
    log.debug("SecurityContext를 세션에 저장 완료 - sessionId: {}",
              request.getSession(false).getId());

    // 3. ✅ HttpSession 명시적 생성 (JSESSIONID 쿠키 생성)
    HttpSession session = request.getSession(true);
    log.debug("HttpSession 생성 완료 - sessionId: {}", session.getId());

    // 4. SuccessHandler 호출
    getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
}
```

**장점:**
- 부모 클래스의 표준 흐름 유지
- `successfulAuthentication()` 호출 자동화
- SecurityContext 세션 저장 명시적

### 방법 2: LocalAuthenticationSuccessHandler에서 세션 저장

```java
@Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                   Authentication authentication) throws IOException, ServletException {
    log.info("LocalAuthenticationSuccessHandler: 로그인 성공 - email: {}",
             authentication.getName());

    // 1. SecurityContext에 Authentication 저장
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);
    log.debug("SecurityContext에 Authentication 저장 완료");

    // 2. ✅ HttpSessionSecurityContextRepository를 사용하여 세션에 저장 (필수!)
    HttpSessionSecurityContextRepository securityContextRepository =
            new HttpSessionSecurityContextRepository();
    securityContextRepository.saveContext(context, request, response);
    log.debug("SecurityContext를 세션에 저장 완료");

    // 3. HttpSession 명시적 생성
    HttpSession session = request.getSession(true);
    log.debug("HttpSession 생성 완료 - sessionId: {}", session.getId());

    // 4. 회원 정보 조회 및 응답
    // ... (기존 코드)
}
```

**장점:**
- 현재 구조 유지
- 명확한 책임 분리

---

## ✅ 권장 해결책: 방법 1

### 이유

1. **표준 패턴 준수**: Spring Security의 표준 흐름 따름
2. **자동화**: `successfulAuthentication()` 자동 호출
3. **코드 간결성**: SuccessHandler는 응답만 담당
4. **유지보수성**: 향후 다른 인증 메커니즘 추가 시 일관성 유지

### 구현 계획

```
JsonUsernamePasswordAuthenticationFilter
├─ attemptAuthentication() ← 현재 상태 유지
├─ successfulAuthentication() ← 추가 (새로운 메서드)
│  ├─ SecurityContext 생성
│  ├─ HttpSessionSecurityContextRepository.saveContext() ← 핵심!
│  └─ SuccessHandler 호출
└─ failedAuthentication() ← (선택사항) 추가
   └─ FailureHandler 호출
```

---

## 📊 비교 테이블

| 항목 | 현재 (문제) | 해결 후 |
|------|-----------|--------|
| **SecurityContext 생성** | ✅ O | ✅ O |
| **Authentication 저장** | ✅ O | ✅ O |
| **세션 저장** | ❌ X | ✅ O (HttpSessionSecurityContextRepository) |
| **JSESSIONID 쿠키** | ✅ O | ✅ O |
| **successfulAuthentication 호출** | ❌ X | ✅ O |
| **다음 요청 인증 상태** | ❌ 미인증 | ✅ 인증됨 |

---

## 🔍 검증 방법

### 로그 확인

```
현재 (문제):
JsonUsernamePasswordAuthenticationFilter: 인증 토큰 생성 완료
LocalAuthenticationSuccessHandler: 로그인 성공
LocalAuthenticationSuccessHandler: SecurityContext에 Authentication 저장 완료
LocalAuthenticationSuccessHandler: HttpSession 생성 완료 - sessionId: abc123

다음 요청:
SecurityContext: null ← ❌ 문제!
```

```
해결 후 (정상):
JsonUsernamePasswordAuthenticationFilter: 인증 성공
JsonUsernamePasswordAuthenticationFilter: SecurityContext에 Authentication 저장 완료
JsonUsernamePasswordAuthenticationFilter: SecurityContext를 세션에 저장 완료
JsonUsernamePasswordAuthenticationFilter: HttpSession 생성 완료 - sessionId: abc123
LocalAuthenticationSuccessHandler: 로그인 성공
LocalAuthenticationSuccessHandler: 로그인 응답 전송 완료

다음 요청:
SecurityContext: Authentication ← ✅ 정상!
```

---

## 📌 핵심 코드 (HttpSessionSecurityContextRepository)

```java
// Spring Security에서 제공하는 클래스
HttpSessionSecurityContextRepository repository =
    new HttpSessionSecurityContextRepository();

// 세션에 SecurityContext 저장 (매우 중요!)
repository.saveContext(context, request, response);

// 결과:
// - request.getSession()에 SPRING_SECURITY_CONTEXT 속성으로 저장됨
// - 다음 요청에서 자동으로 복원됨
```

---

## 🎯 구현 방향

1. **JsonUsernamePasswordAuthenticationFilter에 `successfulAuthentication()` 메서드 추가**
2. **HttpSessionSecurityContextRepository 사용으로 세션 저장**
3. **LocalAuthenticationSuccessHandler는 응답 처리에만 집중**
4. **테스트: 로그인 후 다음 요청에서 인증 상태 확인**

