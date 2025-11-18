# JsonUsernamePasswordAuthenticationFilter Bean 생성 에러 분석 (2025-11-18 재분석)

## 문제 요약

애플리케이션 시작 시 다음 에러 발생:

```
Error creating bean with name 'jsonUsernamePasswordAuthenticationFilter':
authenticationManager must be specified
```

**발생 위치:** Bean 생성 단계 (afterPropertiesSet 검증)
**최근 에러 로그:** request.md 라인 67-69

---

## 근본 원인

### 🔴 **문제 1: LocalAuthConfig에서 authenticationManager를 필터에 설정하지 않음**

**LocalAuthConfig.java 라인 52-59:**

```java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    return filter;  // ❌ authenticationManager를 설정하지 않음!
}
```

**문제점:**
- `authenticationManager` 파라미터를 받음 ✓
- 하지만 필터에 설정하지 않음 ✗
- `JsonUsernamePasswordAuthenticationFilter`는 `UsernamePasswordAuthenticationFilter` 상속
- 이는 `AbstractAuthenticationProcessingFilter` 상속
- `afterPropertiesSet()`에서 `authenticationManager`가 null이면 에러 발생

**증거 (request.md 라인 67-69):**
```
java.lang.IllegalArgumentException: authenticationManager must be specified
  at org.springframework.util.Assert.notNull(Assert.java:181)
  at org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter.afterPropertiesSet(...)
```

### 🔴 **문제 2: SecurityConfig의 설정은 너무 늦음**

**SecurityConfig.java 라인 197:**

```java
jsonLocalLoginFilter.setAuthenticationManager(authenticationManager);
```

**문제:**
- Bean은 **LocalAuthConfig에서 생성** 됨
- 생성 직후 `afterPropertiesSet()` 호출 → **에러 발생**
- SecurityConfig의 설정은 bean 이미 망가진 후 실행 됨

**타이밍:**
```
1. LocalAuthConfig에서 bean 생성
   ↓
2. afterPropertiesSet() 호출 → authenticationManager null 검사
   ↓
3. 💥 IllegalArgumentException 발생!
   ↓
4. SecurityConfig.securityFilterChain() 실행 ← 여기 도달 안 함
   (jsonLocalLoginFilter.setAuthenticationManager(...) ← 이 코드 실행 안 됨)
```

---

## 해결 방법

### ✅ **LocalAuthConfig.java 수정**

**현재 코드 (라인 52-59):**
```java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    return filter;  // ❌
}
```

**수정 코드:**
```java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    filter.setAuthenticationManager(authenticationManager);  // ✅ 추가: authenticationManager 설정
    return filter;
}
```

**변경 내용:**
- 라인 58 다음에 `filter.setAuthenticationManager(authenticationManager);` 추가
- **단 한 줄!**

---

## 왜 이 문제가 발생했는가?

### Spring Bean의 생명주기 (InitializingBean 패턴)

```
[1] Spring이 LocalAuthConfig 감지
    ↓
[2] jsonUsernamePasswordAuthenticationFilter() 메서드 실행
    ├─ AuthenticationManager 주입됨 ✓
    ├─ new JsonUsernamePasswordAuthenticationFilter(objectMapper) 생성
    └─ return filter

    ↓
[3] Bean 등록 직후 afterPropertiesSet() 자동 호출 ⏰
    │ (필터가 AbstractAuthenticationProcessingFilter 상속이므로)
    │
    └─ AbstractAuthenticationProcessingFilter.afterPropertiesSet()
       ├─ authenticationManager 검증: Assert.notNull(this.authenticationManager, ...)
       ├─ this.authenticationManager = null? ✗
       └─ IllegalArgumentException 발생! 💥

    ↓
[4] Bean 생성 실패
    ├─ BeanCreationException 발생
    ├─ 애플리케이션 시작 중단
    └─ 에러 로그 출력
```

### "주입받은 파라미터"와 "필터가 사용하는 필드"는 다름

```java
// ❌ 잘못된 이해
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager  // ← 파라미터로 받음
) {
    // authenticationManager가 준비되어 있으니 필터도 자동으로 사용할 것 같지만
    // 실제로는 필터의 필드 (this.authenticationManager)에 설정해야 함
}

// ✅ 올바른 이해
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager  // ← 파라미터로 받음
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);

    filter.setAuthenticationManager(authenticationManager);  // ← 필터에 설정
    // 이제 filter.getAuthenticationManager() != null ✓

    return filter;
}
```

---

## 현재 코드 상태 확인

| 파일 | 라인 | 클래스 | 메서드 | 상태 | 문제 |
|------|------|--------|--------|------|------|
| LocalAuthConfig.java | 40-42 | LocalAuthConfig | authenticationManager() | ✅ OK | AuthenticationManager bean 정상 생성 |
| **LocalAuthConfig.java** | **52-59** | **LocalAuthConfig** | **jsonUsernamePasswordAuthenticationFilter()** | **❌ BUG** | **authenticationManager를 필터에 설정하지 않음** |
| SecurityConfig.java | 197 | SecurityConfig | securityFilterChain() | ⏰ 늦음 | 필터 설정 시도하지만 bean 이미 실패 후 |
| JsonUsernamePasswordAuthenticationFilter.java | 38-45 | JsonUsernamePasswordAuthenticationFilter | constructor | ✅ OK | ObjectMapper만 받음 (정상) |

---

## 해결 후 결과

### ✅ 수정 전:
```
Bean 생성 → authenticationManager = null → afterPropertiesSet() → 💥 에러
```

### ✅ 수정 후:
```
Bean 생성 → filter.setAuthenticationManager(authenticationManager)
    → authenticationManager ≠ null
    → afterPropertiesSet() 통과 ✓
    → Bean 등록 완료 ✓
```

---

## 요점 정리

| 항목 | 설명 |
|------|------|
| **에러 메시지** | `authenticationManager must be specified` |
| **에러 발생 위치** | `AbstractAuthenticationProcessingFilter.afterPropertiesSet()` (라인 199) |
| **근본 원인** | LocalAuthConfig에서 authenticationManager를 필터에 설정하지 않음 |
| **해결책** | `filter.setAuthenticationManager(authenticationManager);` 한 줄 추가 |
| **수정 파일** | `LocalAuthConfig.java` |
| **수정 라인** | 58 다음 (또는 return 전) |
| **난이도** | ⭐ (1줄) |

---

## 추가 질문: "왜 SecurityConfig에서 설정해도 안 되는가?"

**SecurityConfig.java 라인 197:**
```java
jsonLocalLoginFilter.setAuthenticationManager(authenticationManager);
```

### 왜 이것이 도움이 안 되는가?

```
[1단계] Spring이 LocalAuthConfig 스캔
    └─ jsonUsernamePasswordAuthenticationFilter() 메서드 발견

[2단계] Bean 생성 및 초기화
    ├─ JsonUsernamePasswordAuthenticationFilter 인스턴스 생성
    ├─ afterPropertiesSet() 자동 호출 ⏰
    └─ 💥 authenticationManager null이므로 에러!

[3단계] Bean 등록 실패 ❌
    └─ Exception이 throw됨
    └─ 애플리케이션 시작 중단
    └─ SecurityConfig.securityFilterChain() 실행 안 됨 ❌
```

**결론: 에러가 발생하면 다음 코드에 도달하지 않음**

---

## 확인: Bean 생성 순서

Spring은 다음 순서로 bean을 처리합니다:

1. **Bean 정의 스캔** → LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter()
2. **메서드 실행** → new JsonUsernamePasswordAuthenticationFilter(...)
3. **Bean 등록** → 스프링 컨테이너에 저장
4. **초기화 콜백** → afterPropertiesSet() 자동 호출 ⏰
   - 이 단계에서 검증 발생!
5. **의존성 주입 완료** → 다른 bean에서 주입 가능

**문제: 4번 단계에서 authenticationManager = null → 에러**

---

## 수정 후 작동 흐름

```java
// ✅ LocalAuthConfig.java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);

    filter.setAuthenticationManager(authenticationManager);  // ✅ 설정!
    // 이제 filter 인스턴스의 authenticationManager 필드가 설정됨

    return filter;
}
```

```
[1] 필터 생성
[2] authenticationManager 필드 설정 ✓
[3] afterPropertiesSet() 호출
    ├─ Assert.notNull(this.authenticationManager, ...)
    ├─ this.authenticationManager ≠ null ✓
    └─ 통과! ✓
[4] Bean 등록 완료 ✓
[5] SecurityConfig에서 추가 설정 (원하면) ✓
[6] 애플리케이션 시작 성공 ✓
```

---

## 참고: SecurityConfig의 추가 설정

**SecurityConfig.java 라인 184-199:**

```java
// 🔧 SecurityContextRepository 설정 (이것은 정상)
http.securityContext(securityContext ->
    securityContext.securityContextRepository(securityContextRepository())
);

// 필터 설정 (추가 설정)
jsonLocalLoginFilter.setAuthenticationSuccessHandler(localSuccessHandler);
jsonLocalLoginFilter.setAuthenticationFailureHandler(localFailureHandler);
jsonLocalLoginFilter.setAuthenticationManager(authenticationManager);  // ← 이것도 좋음
jsonLocalLoginFilter.setSecurityContextRepository(securityContextRepository());
```

**참고:**
- LocalAuthConfig에서 authenticationManager를 설정하면
- SecurityConfig에서 다시 설정해도 됨 (덮어쓰기)
- 하지만 필수는 아님 (LocalAuthConfig에서만 설정해도 충분)

---

## 최종 결론

**LocalAuthConfig의 `jsonUsernamePasswordAuthenticationFilter()` 메서드에서 authenticationManager를 필터에 명시적으로 설정하세요:**

```java
filter.setAuthenticationManager(authenticationManager);
```

**이 한 줄로 모든 문제가 해결됩니다.** ✅

---

---

# 추가 분석: AuthenticationManager vs LocalSuccessHandler vs SecurityContextRepository

## 당신의 질문

> "왜 `localSuccessHandler`나 `SecurityContextRepository`는 SecurityConfig에서 setter로 filter에 주입해도 문제가 없으나 AuthenticationManager는 setter로 주입하면 안 되는가?"

### 좋은 질문입니다! 🎯

이것은 **Spring Bean의 생명주기**와 **초기화 검증**의 차이를 이해하는 핵심 개념입니다.

---

## 핵심 답변

| 항목 | AuthenticationManager | LocalSuccessHandler | SecurityContextRepository |
|------|---------------------|---------------------|------------------------|
| **Bean 생성 후 검증** | ✅ **afterPropertiesSet()에서 검증** | ❌ 검증 없음 | ❌ 검증 없음 |
| **타이밍** | Bean 생성 직후 | Bean 생성 후 (검증 없음) | Bean 생성 후 (검증 없음) |
| **SecurityConfig 설정 때** | 이미 검증 실패 | 검증이 없으므로 OK | 검증이 없으므로 OK |
| **결론** | LocalAuthConfig에서 설정 필수 ⚠️ | SecurityConfig에서 설정 OK | SecurityConfig에서 설정 OK |

---

## 상세 분석

### 1️⃣ AuthenticationManager (⚠️ 주의!)

**현재 코드 (LocalAuthConfig.java 라인 52-59):**
```java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    return filter;  // ❌ authenticationManager 미설정
}
```

**Bean 생성 순서:**
```
[1] JsonUsernamePasswordAuthenticationFilter 인스턴스 생성
[2] Spring이 자동으로 afterPropertiesSet() 호출 ⏰ ← 여기가 핵심!
    │
    └─ AbstractAuthenticationProcessingFilter.afterPropertiesSet()
       ├─ Assert.notNull(this.authenticationManager, "authenticationManager must be specified")
       ├─ this.authenticationManager = null? ✗
       └─ 💥 IllegalArgumentException 발생!
[3] Bean 등록 실패 ❌
[4] SecurityConfig 설정 실행 안 됨 ❌
```

**Spring Security 소스코드 (AbstractAuthenticationProcessingFilter.java):**
```java
public abstract class AbstractAuthenticationProcessingFilter extends GenericFilterBean
        implements ApplicationEventPublisherAware, MessageSourceAware {

    protected AuthenticationManager authenticationManager;

    public void afterPropertiesSet() throws ServletException {
        // ⏰ Bean 생성 직후 자동 호출
        Assert.notNull(this.authenticationManager, "authenticationManager must be specified");
        // ↑ null이면 즉시 에러 발생!
    }
}
```

**결론: afterPropertiesSet()는 Bean 생성 직후 자동 호출되므로, SecurityConfig의 설정은 이미 실패한 후**

---

### 2️⃣ LocalAuthenticationSuccessHandler (✅ 안전!)

**LocalAuthenticationSuccessHandler.java 라인 33:**
```java
@Component
@RequiredArgsConstructor
public class LocalAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    // ← 검증 메서드 없음!

    @Override
    public void onAuthenticationSuccess(...) {
        // 처리 로직
    }
}
```

**특징:**
- `AuthenticationSuccessHandler` 인터페이스 구현
- `afterPropertiesSet()` 메서드 없음 ❌
- **Bean 생성 후 검증이 없음** ✓

**SecurityConfig에서 설정 (라인 195):**
```java
jsonLocalLoginFilter.setAuthenticationSuccessHandler(localSuccessHandler);
```

**타이밍:**
```
[1] LocalAuthenticationSuccessHandler Bean 생성
    ├─ afterPropertiesSet() 호출? NO ❌
    └─ 검증? NO ❌
[2] Bean 등록 완료 ✓
[3] SecurityConfig 실행
    ├─ jsonLocalLoginFilter.setAuthenticationSuccessHandler(localSuccessHandler)
    └─ ✓ 안전! (검증이 없으므로)
```

**결론: Handler는 검증이 없으므로 SecurityConfig에서 설정해도 OK**

---

### 3️⃣ SecurityContextRepository (✅ 안전!)

**SecurityConfig.java 라인 142-145:**
```java
@Bean
public SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
    // ← 검증 메서드 없음!
}
```

**특징:**
- `SecurityContextRepository` 인터페이스 구현
- `afterPropertiesSet()` 메서드 없음 ❌
- **Bean 생성 후 검증이 없음** ✓

**SecurityConfig에서 설정 (라인 198):**
```java
jsonLocalLoginFilter.setSecurityContextRepository(securityContextRepository());
```

**타이밍:**
```
[1] HttpSessionSecurityContextRepository Bean 생성
    ├─ afterPropertiesSet() 호출? NO ❌
    └─ 검증? NO ❌
[2] Bean 등록 완료 ✓
[3] SecurityConfig 실행
    ├─ jsonLocalLoginFilter.setSecurityContextRepository(...)
    └─ ✓ 안전! (검증이 없으므로)
```

**결론: SecurityContextRepository는 검증이 없으므로 SecurityConfig에서 설정해도 OK**

---

## 비교 테이블

| 항목 | AuthenticationManager | LocalSuccessHandler | SecurityContextRepository |
|------|---------------------|---------------------|------------------------|
| **클래스** | `AuthenticationManager` (Spring Security) | `LocalAuthenticationSuccessHandler` (Custom) | `HttpSessionSecurityContextRepository` (Spring Security) |
| **afterPropertiesSet()** | ✅ **있음** (부모: AbstractAuthenticationProcessingFilter) | ❌ 없음 | ❌ 없음 |
| **Bean 생성 후 검증** | ✅ **자동 호출** | ❌ 호출 안 함 | ❌ 호출 안 함 |
| **검증 내용** | `authenticationManager != null` 검사 | - | - |
| **LocalAuthConfig 설정** | ✅ **필수!** | ℹ️ 선택 | ℹ️ 선택 |
| **SecurityConfig 설정** | ❌ **너무 늦음** | ✅ OK | ✅ OK |

---

## 핵심 개념: InitializingBean 인터페이스

### Spring은 Bean 생성 후 자동으로 초기화 메서드를 호출합니다

```java
// InitializingBean 인터페이스
public interface InitializingBean {
    void afterPropertiesSet() throws Exception;
    // ↑ Bean 생성 직후 Spring이 자동 호출
}
```

### AbstractAuthenticationProcessingFilter는 이 인터페이스를 구현

```java
public abstract class AbstractAuthenticationProcessingFilter
        extends GenericFilterBean          // ← GenericFilterBean 상속
        implements ApplicationEventPublisherAware {

    // GenericFilterBean → InitializingBean 구현

    public void afterPropertiesSet() throws ServletException {
        // Spring이 자동으로 호출
        Assert.notNull(this.authenticationManager, "authenticationManager must be specified");
    }
}
```

### JsonUsernamePasswordAuthenticationFilter의 상속 구조

```
JsonUsernamePasswordAuthenticationFilter
  ↓ extends
UsernamePasswordAuthenticationFilter
  ↓ extends
AbstractAuthenticationProcessingFilter
  ↓ extends
GenericFilterBean
  ↓ implements
InitializingBean ← afterPropertiesSet() 호출!
```

### LocalAuthenticationSuccessHandler의 상속 구조

```
LocalAuthenticationSuccessHandler
  ↓ implements
AuthenticationSuccessHandler
  ↓ (no afterPropertiesSet())
```

---

## Bean 생성 순서 정리

### ✅ AuthenticationManager (LocalAuthConfig 필수)

```
[1] LocalAuthConfig 클래스 스캔
[2] jsonUsernamePasswordAuthenticationFilter() 메서드 감지
[3] 메서드 실행:
    ├─ new JsonUsernamePasswordAuthenticationFilter(objectMapper) 생성
    └─ return filter

[4] ⏰ Bean 등록 시작
    ├─ GenericFilterBean 상속 확인
    ├─ InitializingBean 구현 확인
    └─ afterPropertiesSet() 자동 호출 ⏰

[5] 🔍 afterPropertiesSet() 검증
    ├─ Assert.notNull(this.authenticationManager, ...)
    ├─ this.authenticationManager = null? ✗
    └─ 💥 IllegalArgumentException!

[6] 에러 발생 ❌
    ├─ BeanCreationException
    ├─ 애플리케이션 시작 중단
    └─ SecurityConfig 실행 안 됨

✗ 실패: SecurityConfig에서 설정 불가능
```

### ✅ LocalSuccessHandler (SecurityConfig 안전)

```
[1] LocalAuthenticationSuccessHandler 클래스 스캔
[2] LocalAuthenticationSuccessHandler 인스턴스 생성

[3] ⏰ Bean 등록 시작
    ├─ AuthenticationSuccessHandler 구현 확인
    ├─ InitializingBean 구현? NO ❌
    └─ afterPropertiesSet() 호출? NO ❌

[4] 검증 없음 ✓
    └─ Bean 등록 완료 ✓

[5] SecurityConfig 실행
    ├─ jsonLocalLoginFilter.setAuthenticationSuccessHandler(localSuccessHandler)
    └─ ✓ 안전!

✓ 성공: SecurityConfig에서 설정 가능
```

### ✅ SecurityContextRepository (SecurityConfig 안전)

```
[1] HttpSessionSecurityContextRepository 인스턴스 생성

[2] ⏰ Bean 등록 시작
    ├─ SecurityContextRepository 구현 확인
    ├─ InitializingBean 구현? NO ❌
    └─ afterPropertiesSet() 호출? NO ❌

[3] 검증 없음 ✓
    └─ Bean 등록 완료 ✓

[4] SecurityConfig 실행
    ├─ jsonLocalLoginFilter.setSecurityContextRepository(securityContextRepository())
    └─ ✓ 안전!

✓ 성공: SecurityConfig에서 설정 가능
```

---

## 코드 예시로 이해하기

### ❌ 왜 SecurityConfig의 설정이 도움이 안 되는가?

**LocalAuthConfig (먼저 실행):**
```java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
        new JsonUsernamePasswordAuthenticationFilter(objectMapper);
    // 여기서 authenticationManager를 설정하지 않음
    return filter;
    // ↓ Spring이 자동으로 afterPropertiesSet() 호출
    // ↓ authenticationManager = null → 💥 에러!
}
```

**SecurityConfig (나중에 실행):**
```java
@Bean
public SecurityFilterChain securityFilterChain(...) throws Exception {
    // ... 생략 ...

    jsonLocalLoginFilter.setAuthenticationManager(authenticationManager);
    // ↑ 이 코드는 실행되지 않음
    // (위에서 이미 Bean 생성 실패)
}
```

### ✅ 올바른 방법 (LocalAuthConfig에서 설정)

**LocalAuthConfig:**
```java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
        new JsonUsernamePasswordAuthenticationFilter(objectMapper);

    filter.setAuthenticationManager(authenticationManager);  // ✅ 여기서 설정!

    return filter;
    // ↓ Spring이 자동으로 afterPropertiesSet() 호출
    // ↓ authenticationManager ≠ null → ✓ 통과!
}
```

---

## 최종 답변

### Q: "왜 다른 것들은 SecurityConfig에서 설정해도 되는데 AuthenticationManager는 안 되나?"

### A: **afterPropertiesSet() 때문입니다**

1. **AuthenticationManager를 필요로 하는 필터 (AbstractAuthenticationProcessingFilter)**
   - `afterPropertiesSet()` 메서드 있음
   - Bean 생성 직후 **자동으로 검증** 실행
   - **SecurityConfig의 설정보다 먼저 검증됨**
   - 따라서 LocalAuthConfig에서 필수 설정

2. **LocalSuccessHandler**
   - `afterPropertiesSet()` 메서드 없음
   - Bean 생성 후 검증 없음
   - SecurityConfig에서 설정 가능

3. **SecurityContextRepository**
   - `afterPropertiesSet()` 메서드 없음
   - Bean 생성 후 검증 없음
   - SecurityConfig에서 설정 가능

### 결론

**필터가 InitializingBean을 구현하고 afterPropertiesSet()에서 검증을 수행하는 경우, 그 검증 전에 필요한 의존성은 LocalAuthConfig(Bean 생성 메서드)에서 설정해야 합니다.**

---

## 예방 팁

### Spring Security 커스텀 필터를 만들 때:

1. **필터의 필수 의존성 파악**
   ```java
   public abstract class AbstractAuthenticationProcessingFilter {
       public void afterPropertiesSet() throws ServletException {
           Assert.notNull(this.authenticationManager, "authenticationManager must be specified");
           // ← authenticationManager가 필수!
       }
   }
   ```

2. **Bean 생성 메서드에서 필수 의존성 설정**
   ```java
   @Bean
   public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(...) {
       JsonUsernamePasswordAuthenticationFilter filter = new JsonUsernamePasswordAuthenticationFilter(...);
       filter.setAuthenticationManager(authenticationManager);  // ✅ 필수!
       return filter;
   }
   ```

3. **선택적 의존성은 SecurityConfig에서 설정 가능**
   ```java
   @Bean
   public SecurityFilterChain securityFilterChain(...) throws Exception {
       jsonLocalLoginFilter.setAuthenticationSuccessHandler(handler);  // ✅ OK
       jsonLocalLoginFilter.setSecurityContextRepository(repository);  // ✅ OK
       return http.build();
   }
   ```

---

---

# 상세 순서 분석: Bean 생성 → Bean 검증 → securityFilterChain 메서드 실행

## 당신의 질문

> "타이밍 순서가 아직도 이해가 안 가. Bean 생성 -> Bean 검증 -> securityFilterChain method 실행 순서야?"

### ✅ 정답: 그 순서가 맞습니다! 🎯

---

## 정확한 실행 순서 (타임라인)

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    Spring 애플리케이션 시작                              │
└─────────────────────────────────────────────────────────────────────────┘
                              ↓

┌─ [T1] Bean 스캔 단계 ──────────────────────────────────────────────────┐
│                                                                         │
│  Spring이 @Configuration 클래스들을 스캔합니다.                         │
│                                                                         │
│  스캔 순서:                                                             │
│  1. LocalAuthConfig 클래스 발견 ✓                                      │
│  2. SecurityConfig 클래스 발견 ✓                                       │
│                                                                         │
│  "LocalAuthConfig와 SecurityConfig에 @Bean이 있네?"                    │
│  "LocalAuthConfig를 먼저 처리해야겠다"                                 │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
                              ↓

┌─ [T2] LocalAuthConfig Bean 생성 단계 ─────────────────────────────────┐
│                                                                         │
│  LocalAuthConfig의 @Bean 메서드들을 실행합니다.                        │
│                                                                         │
│  ┌─ [T2-1] authenticationManager() Bean 생성                          │
│  │  ┌──────────────────────────────────────────────────────────┐      │
│  │  │ @Bean                                                    │      │
│  │  │ public AuthenticationManager authenticationManager(     │      │
│  │  │         AuthenticationConfiguration authConfig          │      │
│  │  │ ) {                                                      │      │
│  │  │     return authConfig.getAuthenticationManager();        │      │
│  │  │ }                                                        │      │
│  │  └──────────────────────────────────────────────────────────┘      │
│  │                                                                     │
│  │  실행:                                                              │
│  │  → authenticationManager Bean 생성 ✓                              │
│  │  → Spring Container에 등록 ✓                                      │
│  │  → afterPropertiesSet() 호출 (검증 없음 - 이것은 AuthMgr가 아님)   │
│  │  → 완료! ✓                                                        │
│  └─────────────────────────────────────────────────────────────────┘
│
│  ┌─ [T2-2] jsonUsernamePasswordAuthenticationFilter() Bean 생성 ⏰ 핵심!
│  │  ┌──────────────────────────────────────────────────────────┐      │
│  │  │ @Bean                                                    │      │
│  │  │ public JsonUsernamePasswordAuthenticationFilter         │      │
│  │  │     jsonUsernamePasswordAuthenticationFilter(           │      │
│  │  │         AuthenticationManager authenticationManager     │      │
│  │  │ ) {                                                      │      │
│  │  │     JsonUsernamePasswordAuthenticationFilter filter =   │      │
│  │  │         new JsonUsernamePasswordAuthenticationFilter(   │      │
│  │  │             objectMapper);                              │      │
│  │  │     return filter;  ❌ authenticationManager 설정 안 함! │      │
│  │  │ }                                                        │      │
│  │  └──────────────────────────────────────────────────────────┘      │
│  │                                                                     │
│  │  실행:                                                              │
│  │  1️⃣ 메서드 호출                                                     │
│  │      authenticationManager 파라미터 주입됨 ✓                        │
│  │      new JsonUsernamePasswordAuthenticationFilter(...)생성 ✓       │
│  │      return filter                                                 │
│  │                                                                     │
│  │  2️⃣ Filter 인스턴스가 Spring Container에 등록 시작                 │
│  │      ↓                                                             │
│  │  3️⃣ ⏰ Spring이 자동으로 afterPropertiesSet() 호출 ⏰ ★ 핵심!      │
│  │      │                                                             │
│  │      ├─ JsonUsernamePasswordAuthenticationFilter                  │
│  │      │   extends UsernamePasswordAuthenticationFilter             │
│  │      │   extends AbstractAuthenticationProcessingFilter           │
│  │      │   extends GenericFilterBean                               │
│  │      │   implements InitializingBean ← 이것 때문에 호출됨!        │
│  │      │                                                             │
│  │      └─ AbstractAuthenticationProcessingFilter.afterPropertiesSet()
│  │         {                                                          │
│  │             Assert.notNull(                                       │
│  │                 this.authenticationManager,                       │
│  │                 "authenticationManager must be specified"         │
│  │             );                                                    │
│  │         }                                                          │
│  │         ↓                                                          │
│  │         🔍 this.authenticationManager = null? ✗                   │
│  │         ↓                                                          │
│  │         💥 IllegalArgumentException 발생!                         │
│  │                                                                     │
│  │  4️⃣ 🛑 Bean 생성 실패! ❌                                          │
│  │      ├─ BeanCreationException 발생                                │
│  │      ├─ Spring Container 초기화 실패                              │
│  │      └─ 애플리케이션 시작 중단!                                   │
│  │                                                                     │
│  │  5️⃣ 🚫 다음 단계 실행 안 됨!                                       │
│  │      ├─ SecurityConfig 로드 안 됨                                  │
│  │      ├─ securityFilterChain() 실행 안 됨                          │
│  │      ├─ jsonLocalLoginFilter.setAuthenticationManager(...) 실행 안 됨
│  │      └─ 에러 로그 출력                                             │
│  │                                                                     │
│  └─────────────────────────────────────────────────────────────────┘
│
│  결과: 🛑 FAILED - 애플리케이션 시작 중단
│
└─────────────────────────────────────────────────────────────────────────┘
                              ↓
                        ❌ 에러 발생!
                     (여기서 멈춤)
                     (다음 단계 진행 안 됨)
                              ↓

┌─ [T3] SecurityConfig Bean 생성 단계 ──────────────────────────────────┐
│                                                                         │
│  🚫 실행되지 않음! (Bean 생성 실패 때문에)                             │
│                                                                         │
│  만약 실행된다면 (T2 성공 시):                                         │
│                                                                         │
│  ┌─ securityFilterChain() 메서드 실행                                 │
│  │  ┌──────────────────────────────────────────────────────────┐      │
│  │  │ @Bean                                                    │      │
│  │  │ public SecurityFilterChain securityFilterChain(         │      │
│  │  │         HttpSecurity http,                              │      │
│  │  │         JsonUsernamePasswordAuthenticationFilter        │      │
│  │  │             jsonLocalLoginFilter,  ← T2에서 생성된 Bean │      │
│  │  │         LocalAuthenticationSuccessHandler               │      │
│  │  │             localSuccessHandler,   ← 이미 생성됨        │      │
│  │  │         ...                                              │      │
│  │  │ ) throws Exception {                                     │      │
│  │  │                                                          │      │
│  │  │     // 여기서 Setter로 설정 시도:                        │      │
│  │  │     jsonLocalLoginFilter.setAuthenticationManager(...); │      │
│  │  │     jsonLocalLoginFilter.setAuthenticationSuccessHandler│      │
│  │  │         (localSuccessHandler);                          │      │
│  │  │     // 등등...                                           │      │
│  │  │                                                          │      │
│  │  │     return http.build();                                │      │
│  │  │ }                                                        │      │
│  │  └──────────────────────────────────────────────────────────┘      │
│  │                                                                     │
│  │  하지만 이 메서드에 도달할 수 없음! (T2 실패 때문)                 │
│  │                                                                     │
│  └─────────────────────────────────────────────────────────────────┘
│
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 타이밍 순서 정리

### ✅ 이론적 순서 (모든 Bean이 성공할 때)

```
1️⃣ [T1] Bean 스캔
2️⃣ [T2-1] LocalAuthConfig.authenticationManager() 실행 → Bean 생성 & 검증 OK
3️⃣ [T2-2] LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter() 실행
          → Bean 생성
          → 검증 (afterPropertiesSet()) ← 이 단계!
          → Bean 등록 완료 (또는 실패)
4️⃣ [T2-3] LocalAuthConfig의 다른 @Bean 메서드들...
5️⃣ [T3] SecurityConfig.securityFilterChain() 실행 ← Setter 설정 가능
6️⃣ 애플리케이션 시작 완료 ✓
```

### ❌ 현재 실제 순서 (현재 버그)

```
1️⃣ [T1] Bean 스캔
2️⃣ [T2-1] LocalAuthConfig.authenticationManager() 실행 → 생성 & 검증 OK ✓
3️⃣ [T2-2] LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter() 실행
          → Bean 생성 ✓
          → 검증 (afterPropertiesSet()) → 💥 실패! ❌
4️⃣ 🛑 STOP! 애플리케이션 시작 중단
5️⃣ [T3] SecurityConfig.securityFilterChain() 실행 ← 도달 불가능 🚫
```

---

## 핵심: 왜 SecurityConfig의 설정은 도움이 안 되는가?

### 시간 순서 (Timeline)

```
시간 →

LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter() 메서드
│
├─ new JsonUsernamePasswordAuthenticationFilter(...) 생성
│  └─ Bean 객체 메모리에 할당 됨
│
├─ 🔴 Spring Container에 등록 시작
│  ├─ InitializingBean 확인 (있음 ✓)
│  └─ afterPropertiesSet() 호출 ⏰
│     │
│     └─ Assert.notNull(this.authenticationManager, ...)
│        │
│        ├─ null? ✗
│        └─ 💥 IllegalArgumentException!
│
└─ 🛑 Bean 등록 실패 (여기서 멈춤)
   └─ 🚫 Exception이 throw됨
      └─ 🚫 애플리케이션 시작 중단
         └─ 🚫 SecurityConfig.securityFilterChain() 도달 불가능


SecurityConfig.securityFilterChain() 메서드
│
└─ (실행되지 않음 - 위에서 이미 실패했으므로)
   │
   ├─ jsonLocalLoginFilter.setAuthenticationManager(...) ← 실행 안 됨
   │  (이 코드에 도달하지 못함)
   │
   └─ X 타임아웃
```

### 순서의 핵심

```
┌─────────────────────────────────────┐
│ 1️⃣ LocalAuthConfig의 @Bean 메서드들 │ ← 먼저 실행
│    (모든 메서드)                      │
│                                       │
│    authenticationManager()            │
│    → Bean 생성 ✓                     │
│                                       │
│    jsonUsernamePasswordAuthenticationFilter()
│    → Bean 생성 ✓                     │
│    → 검증 (afterPropertiesSet()) 💥 │ ← 여기서 실패!
│    → 🛑 애플리케이션 중단             │
└─────────────────────────────────────┘
            🚫 다음 진행 안 됨

┌─────────────────────────────────────┐
│ 2️⃣ SecurityConfig의 @Bean 메서드들  │ ← 나중에 실행
│    (모든 메서드)                      │
│                                       │
│    securityFilterChain()              │
│    → 실행되지 않음 🚫                 │
│    (Bean이 이미 실패했으므로)        │
│                                       │
│    jsonLocalLoginFilter.set...()      │
│    → 호출되지 않음 🚫                 │
└─────────────────────────────────────┘
```

---

## 수정 후 타이밍 순서

### ✅ 수정된 코드

```java
// LocalAuthConfig.java
@Bean
public JsonUsernamePasswordAuthenticationFilter jsonUsernamePasswordAuthenticationFilter(
        AuthenticationManager authenticationManager
) {
    JsonUsernamePasswordAuthenticationFilter filter =
            new JsonUsernamePasswordAuthenticationFilter(objectMapper);

    filter.setAuthenticationManager(authenticationManager);  // ✅ 추가!

    return filter;
}
```

### ✅ 수정 후 타이밍

```
시간 →

LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter() 메서드
│
├─ new JsonUsernamePasswordAuthenticationFilter(...) 생성
│  └─ Bean 객체 메모리에 할당 됨
│
├─ filter.setAuthenticationManager(authenticationManager) ✅ 설정!
│  └─ this.authenticationManager = authenticationManager (not null)
│
├─ 🟢 Spring Container에 등록 시작
│  ├─ InitializingBean 확인 (있음 ✓)
│  └─ afterPropertiesSet() 호출 ⏰
│     │
│     └─ Assert.notNull(this.authenticationManager, ...)
│        │
│        ├─ null? ✓ (설정했으므로!)
│        └─ ✅ 검증 통과!
│
├─ ✅ Bean 등록 완료!
│  └─ Spring Container에 저장
│
└─ ✅ 다음 Bean으로 진행

         ↓ (시간이 계속 흘러감)

SecurityConfig.securityFilterChain() 메서드
│
└─ ✅ 실행됨! (Bean 생성 성공했으므로)
   │
   ├─ jsonLocalLoginFilter.setAuthenticationManager(...) ✅ 실행됨
   │  (이미 설정되어 있지만, 덮어쓰기 가능)
   │
   ├─ jsonLocalLoginFilter.setAuthenticationSuccessHandler(...) ✅ 실행됨
   │
   └─ ✅ 애플리케이션 시작 성공
```

---

## 시각적 비교: 현재 vs 수정 후

### ❌ 현재 (버그)

```
시간 축 →

LocalAuthConfig                          SecurityConfig
│                                        │
├─ authenticationManager() ✓              │
│                                        │
├─ jsonUsernamePassword...() 🛑 실패!    │
│  └─ afterPropertiesSet() 💥             │
│     └─ authenticationManager = null     │
│        └─ 에러!                         │
│                                        │
└─ 🚫 애플리케이션 중단                   └─ 🚫 실행 안 됨
   (여기서 멈춤!)                         (도달 불가능)
```

### ✅ 수정 후 (정상)

```
시간 축 →

LocalAuthConfig                          SecurityConfig
│                                        │
├─ authenticationManager() ✓              │
│                                        │
├─ jsonUsernamePassword...() ✓           │
│  └─ afterPropertiesSet() ✓             │
│     └─ authenticationManager ≠ null    │
│        └─ 통과!                        │
│                                        │
└─ ✅ 완료                               └─ ✅ 실행됨!
                                            ├─ setter 설정
                                            └─ ✅ 애플리케이션 시작 성공
```

---

## 최종 답변

### Q: "Bean 생성 -> Bean 검증 -> securityFilterChain method 실행 순서야?"

### A: **정확히 그 순서가 맞습니다!** ✅

```
[1] Bean 생성
    ↓
[2] Bean 검증 (afterPropertiesSet() 자동 호출)
    ↓ (실패 시 여기서 멈춤!)
    ↓ (성공 시 다음으로)
[3] securityFilterChain() 메서드 실행
```

### 현재 상황

```
[1] ✓ LocalAuthConfig.authenticationManager() Bean 생성 & 검증 OK
    ↓
[2] ✓ LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter() Bean 생성
    ↓
[3] ❌ afterPropertiesSet() 검증 실패! (authenticationManager = null)
    └─ 💥 IllegalArgumentException 발생
    └─ 🛑 애플리케이션 중단
    └─ [4] SecurityConfig.securityFilterChain() 실행 불가능
```

### 수정 후

```
[1] ✓ LocalAuthConfig.authenticationManager() Bean 생성 & 검증 OK
    ↓
[2] ✓ LocalAuthConfig.jsonUsernamePasswordAuthenticationFilter() Bean 생성
    ↓
[3] ✓ afterPropertiesSet() 검증 OK! (authenticationManager ≠ null)
    ↓
[4] ✓ SecurityConfig.securityFilterChain() 실행 가능!
    └─ ✓ 애플리케이션 시작 성공
```
