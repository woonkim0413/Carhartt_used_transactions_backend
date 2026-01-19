# 사용자 제안 평가: AddressControllerTest 수정 방안

## 제안 1: email 값 일치시키기

### 제안 내용
> @WithMockCustomUser annotation의 email 값을 @BeforeEach에서 등록한 값과 같게 변경

### 평가: ❌ 부적절

### 이유

현재 `WithMockCustomUserSecurityContextFactory`의 동작:

```java
// WithMockCustomUserSecurityContextFactory.java:21-34
CustomLocalUser customUser = CustomLocalUser.customBuilder()
    .username(annotation.email())
    .password(annotation.password())
    // ...
    .memberId(annotation.memberId())  // ← 여기서 annotation의 memberId를 직접 사용!
    .email(annotation.email())
    // ...
    .build();
```

**핵심 문제**: Factory는 DB를 조회하지 않고 annotation에서 지정한 `memberId`를 그대로 사용합니다.

```
@WithMockCustomUser(email = "dnsrkd0414@naver.com", memberId = 1L)
                    ↓
Factory가 memberId = 1L로 CustomLocalUser 생성
                    ↓
Controller에서 user.getMemberId() = 1L 반환
                    ↓
AddressUseCase.addAddress(1L, dto) 호출
                    ↓
memberRepository.findById(1L) → DB에 ID=1인 멤버 없음 → 에러!
```

**결론**: email을 일치시켜도 memberId 불일치 문제는 해결되지 않습니다.

---

## 제안 2: @BeforeEach에서 MemberId를 1로 직접 주입

### 제안 내용
> @BeforeEach에서 Member 생성 시 MemberId를 1로 직접 주입

### 평가: ⚠️ 가능하지만 복잡함

### 이유

Member 엔티티의 ID 생성 전략:

```java
// Member.java:39-41
@Id @GeneratedValue(strategy = GenerationType.IDENTITY)
@Column(name = "member_id", nullable = false)
private Long memberId;
```

**`GenerationType.IDENTITY`의 특성**:
- DB가 AUTO_INCREMENT로 ID를 자동 생성
- `save()` 호출 시 INSERT 쿼리 실행 → DB가 ID 할당 → 엔티티에 반영
- Java 코드에서 ID를 직접 설정해도 **무시됨**

### 가능한 방법들

| 방법 | 복잡도 | 권장 |
|------|--------|------|
| Reflection으로 memberId 필드 설정 | 높음 | ❌ |
| @Sql로 ID=1인 멤버 직접 INSERT | 중간 | △ |
| 테스트용 생성자 추가 (프로덕션 코드 변경) | 중간 | ❌ |
| H2 시퀀스 리셋 | 중간 | △ |

### 방법 2-1: @Sql 사용

```java
@Test
@Sql(statements = {
    "DELETE FROM member WHERE member_id = 1",
    "INSERT INTO member (member_id, nickname, member_name, email, login_password, local_provider, login_type) " +
    "VALUES (1, '테스트닉네임', '김운강', 'dnsrkd0414@naver.com', 'encodedPassword123', 'LOCAL', 'LOCAL')"
}, executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@WithMockCustomUser(email = "dnsrkd0414@naver.com", memberId = 1L)
void addAddress_Success() throws Exception {
    // @BeforeEach의 testMember 생성 코드 제거 필요
}
```

**문제점**:
- `@BeforeEach`와 중복 관리
- 모든 테스트 메서드에 @Sql 추가 필요
- SQL 문자열 관리 어려움

### 방법 2-2: H2 시퀀스 리셋

```java
@BeforeEach
void setUp() {
    // H2 시퀀스 리셋 (주의: H2 전용 문법)
    entityManager.createNativeQuery("ALTER TABLE member ALTER COLUMN member_id RESTART WITH 1").executeUpdate();

    testMember = new Member(...);
    memberRepository.save(testMember);
    // 이제 testMember.getMemberId() = 1L
}
```

**문제점**:
- H2 전용 문법 (다른 DB에서 테스트 시 실패)
- 테스트 순서에 따라 불안정할 수 있음
- 다른 테스트 클래스와 충돌 가능

---

## 권장 해결책

### 방법 A: WithMockCustomUserSecurityContextFactory 수정 (권장)

Factory에서 email로 DB의 실제 Member를 조회하도록 수정:

```java
@Component  // Spring Bean으로 등록
public class WithMockCustomUserSecurityContextFactory
        implements WithSecurityContextFactory<WithMockCustomUser> {

    @Autowired
    private MemberRepository memberRepository;

    @Override
    public SecurityContext createSecurityContext(WithMockCustomUser annotation) {
        // email로 실제 Member 조회
        Long actualMemberId = memberRepository.findByEmail(annotation.email())
            .map(Member::getMemberId)
            .orElse(annotation.memberId());  // fallback: annotation 값 사용

        CustomLocalUser customUser = CustomLocalUser.customBuilder()
            .username(annotation.email())
            .password(annotation.password())
            .authorities(...)
            .memberId(actualMemberId)  // ← 실제 DB의 memberId 사용!
            .email(annotation.email())
            .localProvider(annotation.provider())
            .build();

        // ... 나머지 동일
    }
}
```

**장점**:
- 테스트 코드 수정 최소화
- `@WithMockCustomUser` 사용 방식 유지
- email만 맞추면 자동으로 memberId 일치

**주의**: `MemberRepository`에 `findByEmail()` 메서드가 필요합니다.

### 방법 B: 테스트에서 직접 인증 객체 생성

`@WithMockCustomUser` 대신 테스트 내에서 직접 생성:

```java
@Test
void addAddress_Success() throws Exception {
    // given
    CustomOAuth2User mockUser = new CustomOAuth2User(
        testMember.getMemberId(),  // 실제 저장된 ID 사용!
        Map.of("name", testMember.getName(), "email", testMember.getEmail()),
        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
    );
    UsernamePasswordAuthenticationToken auth =
        new UsernamePasswordAuthenticationToken(mockUser, null, mockUser.getAuthorities());

    // when & then
    mockMvc.perform(put("/v1/orders/address")
            .with(authentication(auth))
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(requestBody)))
        .andExpect(status().isOk());
}
```

**장점**:
- Factory 수정 불필요
- 테스트별로 유연한 인증 설정 가능

**단점**:
- 테스트 코드가 다소 길어짐
- 각 테스트에서 반복 코드 발생

---

## 결론

| 제안 | 평가 | 이유 |
|------|------|------|
| **제안 1** (email 일치) | ❌ 부적절 | Factory가 DB를 조회하지 않고 annotation의 memberId를 직접 사용 |
| **제안 2** (memberId 직접 주입) | ⚠️ 가능하지만 복잡 | IDENTITY 전략에서 ID 직접 설정 어려움, H2 전용 해결책 필요 |

### 최종 권장

**방법 A (Factory 수정)** 또는 **방법 B (직접 인증 객체 생성)** 사용

- **빠른 수정**: 방법 B (테스트 코드만 수정)
- **장기적 유지보수**: 방법 A (Factory 수정으로 모든 테스트에 적용)
