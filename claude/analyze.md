# 세션이 생성되지 않는 문제 분석

## 문제 현상
`v1/local/login` API를 성공적으로 실행해도 세션(JSESSIONID 쿠키)이 생성되지 않는 문제

## 원인 분석

### 1. **핵심 원인: LocalAuthenticationSuccessHandler에서 saveRequest() 호출 부재**

Spring Security의 `AuthenticationSuccessHandler`는 기본적으로 `onAuthenticationSuccess()` 메서드를 구현할 때 다음 조건을 만족해야 세션이 생성됩니다:

- **SecurityContext에 Authentication을 저장**
- **HttpSession을 명시적으로 생성 또는 요청**

현재 코드의 문제점:
```java
// LocalAuthenticationSuccessHandler.java - onAuthenticationSuccess() 메서드
@Override
public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                   Authentication authentication) throws IOException, ServletException {
    // ... 로그인 성공 처리 로직 ...
    response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    // ❌ 문제: saveRequest(request, response) 호출 없음
    // ❌ 문제: SecurityContext에 Authentication 저장 안 함
}
```

### 2. **SecurityContext에 Authentication이 저장되지 않음**

`UsernamePasswordAuthenticationFilter`의 기본 동작:
1. `attemptAuthentication()` 호출 → Authentication 반환
2. **기본 `successfulAuthentication()` 메서드가 호출되어 SecurityContext에 저장**
3. `successfulAuthentication()` 내부에서 `getSession(true)` 호출 → **세션 생성**

현재 아키텍처의 문제:
- `LocalAuthenticationSuccessHandler`에서 `onAuthenticationSuccess()` 오버라이드
- 하지만 **`SecurityContext.getContext().setAuthentication(authentication)` 호출 안 함**
- HttpSession 생성 요청도 명시적으로 하지 않음

### 3. **SessionCreationPolicy 설정과의 관계**

SecurityConfig.java:
```java
http.sessionManagement(httpSecuritySessionManagementConfigurer ->
    httpSecuritySessionManagementConfigurer.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
);
```

- `IF_REQUIRED`: 세션이 필요할 때만 생성
- Spring Security 인증 완료 시 자동으로 세션 생성되어야 함
- 하지만 SecurityContext에 Authentication이 저장되지 않으면 "필요함"이 인식되지 않음

### 4. **기본 UsernamePasswordAuthenticationFilter의 동작**

기본 필터의 `successfulAuthentication()` 메서드 (오버라이드 전):
```java
protected void successfulAuthentication(HttpServletRequest request,
                                       HttpServletResponse response,
                                       FilterChain chain,
                                       Authentication authResult) {
    // SecurityContext에 Authentication 저장
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authResult);
    SecurityContextHolder.setContext(context);

    // 세션 생성 트리거
    request.getSession(true);

    // SuccessHandler 호출
    getSuccessHandler().onAuthenticationSuccess(request, response, authResult);
}
```

**현재 코드는 이 과정을 완전히 스킵하고 있음**

## 해결 방안

### 1. **LocalAuthenticationSuccessHandler에 SecurityContext 저장 추가**
```java
SecurityContext context = SecurityContextHolder.createEmptyContext();
context.setAuthentication(authentication);
SecurityContextHolder.setContext(context);
```

### 2. **명시적으로 HttpSession 생성 요청**
```java
HttpSession session = request.getSession(true);
```

### 3. **또는 기본 successfulAuthentication() 메서드 활용**
`UsernamePasswordAuthenticationFilter`를 오버라이드하지 말고, `successfulAuthentication()` 메서드만 오버라이드하여 응답 포맷을 커스터마이징

## 최종 원인 요약

| 항목 | 현재 상태 | 필요한 상태 |
|------|---------|-----------|
| SecurityContext 저장 | ❌ 안 함 | ✅ 필수 |
| HttpSession 생성 요청 | ❌ 안 함 | ✅ 필수 |
| Authentication 객체 | ✅ 유효 | ✅ 유효 |
| 응답 반환 | ✅ JSON으로 성공 반환 | ✅ 유지 |

**결론**: `LocalAuthenticationSuccessHandler`에서 인증 정보를 SecurityContext에 저장하고 명시적으로 세션을 생성해야 함
