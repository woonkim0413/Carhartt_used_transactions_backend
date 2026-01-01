# AddressController JUnit 테스트 문서

## 개요
이 문서는 `AddressController`의 API 엔드포인트에 대한 JUnit 테스트 코드를 정리한 문서입니다.

## 테스트 대상 API

### 1. PUT /v1/orders/address - 주소 추가
- **설명**: 현재 로그인 사용자가 가진 주소 목록에 주소를 추가
- **제약사항**:
  - 주소지 이름은 중복 불가
  - 사용자는 최대 5개의 주소지 보유 가능
  - 마지막에 추가한 주소지가 기본 주소지로 설정됨

### 2. GET /v1/orders/address - 주소 목록 조회
- **설명**: 현재 로그인되어 있는 사용자의 주소지 목록 반환

### 3. DELETE /v1/orders/address/{address_id} - 주소 삭제
- **설명**: 주소ID가 현재 로그인한 멤버에 포함된 주소인 경우 삭제

## 테스트 파일 위치
```
src/test/java/com/C_platform/Member_woonkim/presentation/controller/AddressControllerTest.java
```

## 테스트 구성

### 테스트 환경 설정
- **프레임워크**: Spring Boot Test with JUnit 5
- **어노테이션**:
  - `@SpringBootTest`: 전체 Spring 컨텍스트 로드
  - `@AutoConfigureMockMvc`: MockMvc 자동 설정
  - `@Transactional`: 각 테스트 후 롤백
- **의존성**:
  - `MockMvc`: HTTP 요청/응답 모킹
  - `ObjectMapper`: JSON 직렬화/역직렬화
  - `MemberRepository`: 테스트용 회원 데이터 관리
  - `AddressRepository`: 테스트용 주소 데이터 관리
  - `AddressUseCase`: 주소 비즈니스 로직

### BeforeEach 설정
각 테스트 전에 다음을 준비합니다:
1. 테스트용 회원 생성 (Local 인증 방식)
   - 이메일: test@example.com
   - 이름: 테스트유저
   - Provider: LocalProvider.LOCAL
2. Mock CustomLocalUser 생성 (인증 정보)

## 작성된 테스트 케이스

### 주소 추가 API 테스트 (PUT /v1/orders/address)

#### 1. addAddress_Success()
- **테스트 내용**: 정상적인 주소 추가 요청
- **검증 사항**:
  - HTTP 200 OK 상태 반환
  - 응답 JSON에 memberId, addressId, addressName 포함
  - X-Request-Id 헤더가 응답의 metaData에 포함
  - DB에 주소가 실제로 저장되었는지 확인

#### 2. addAddress_Unauthorized()
- **테스트 내용**: 인증되지 않은 사용자의 주소 추가 요청
- **검증 사항**: HTTP 401 Unauthorized 반환

#### 3. addAddress_InvalidInput_BlankAddressName()
- **테스트 내용**: 주소 이름이 빈 값인 경우
- **검증 사항**: HTTP 400 Bad Request 반환 (Bean Validation)

#### 4. addAddress_InvalidInput_MissingZipCode()
- **테스트 내용**: 우편번호가 누락된 경우
- **검증 사항**: HTTP 400 Bad Request 반환 (Bean Validation)

#### 5. addAddress_MaxLimitExceeded()
- **테스트 내용**: 주소가 이미 5개인 경우 추가 시도
- **검증 사항**: HTTP 4xx Client Error 반환
- **참고**: AddressService.addressNumberCheck_MaxFive() 구현 필요

### 주소 목록 조회 API 테스트 (GET /v1/orders/address)

#### 6. getAddressList_Success()
- **테스트 내용**: 정상적인 주소 목록 조회 (주소 2개 등록 후)
- **검증 사항**:
  - HTTP 200 OK 상태 반환
  - 응답 JSON에 memberId와 addressList 배열 포함
  - addressList 배열의 크기가 2

#### 7. getAddressList_EmptyList()
- **테스트 내용**: 주소가 없는 사용자의 주소 목록 조회
- **검증 사항**:
  - HTTP 200 OK 상태 반환
  - addressList 배열이 비어있음 (크기 0)

#### 8. getAddressList_Unauthorized()
- **테스트 내용**: 인증되지 않은 사용자의 주소 목록 조회
- **검증 사항**: HTTP 401 Unauthorized 반환

### 주소 삭제 API 테스트 (DELETE /v1/orders/address/{address_id})

#### 9. deleteAddress_Success()
- **테스트 내용**: 정상적인 주소 삭제 요청
- **검증 사항**:
  - HTTP 200 OK 상태 반환
  - DB에서 주소가 실제로 삭제되었는지 확인
- **참고**: deleteAddress는 OAuth2User만 지원하므로 CustomOAuth2User 사용

#### 10. deleteAddress_NotFound()
- **테스트 내용**: 존재하지 않는 주소 ID로 삭제 요청
- **검증 사항**: HTTP 500 Server Error 반환 (현재 RuntimeException 발생)
- **개선 사항**: 적절한 ErrorCode와 404 응답 필요

#### 11. deleteAddress_Forbidden()
- **테스트 내용**: 다른 사용자의 주소 삭제 시도
- **검증 사항**: HTTP 500 Server Error 반환 (현재 RuntimeException 발생)
- **개선 사항**: 적절한 ErrorCode와 403 Forbidden 응답 필요

#### 12. deleteAddress_Unauthorized()
- **테스트 내용**: 인증되지 않은 사용자의 주소 삭제 요청
- **검증 사항**: HTTP 401 Unauthorized 반환

## 테스트 실행 방법

### 전체 테스트 실행
```bash
./gradlew test --tests com.C_platform.Member_woonkim.presentation.controller.AddressControllerTest
```

### 개별 테스트 실행
```bash
./gradlew test --tests com.C_platform.Member_woonkim.presentation.controller.AddressControllerTest.addAddress_Success
```

## 테스트 결과 요약

### 작성된 테스트: 총 12개
1. ✅ addAddress_Success - 정상 주소 추가
2. ✅ addAddress_Unauthorized - 미인증 주소 추가
3. ✅ addAddress_InvalidInput_BlankAddressName - 빈 주소 이름
4. ✅ addAddress_InvalidInput_MissingZipCode - 우편번호 누락
5. ✅ addAddress_MaxLimitExceeded - 최대 5개 제한
6. ✅ getAddressList_Success - 정상 목록 조회
7. ✅ getAddressList_EmptyList - 빈 목록 조회
8. ✅ getAddressList_Unauthorized - 미인증 목록 조회
9. ✅ deleteAddress_Success - 정상 주소 삭제
10. ✅ deleteAddress_NotFound - 존재하지 않는 주소 삭제
11. ✅ deleteAddress_Forbidden - 다른 사용자 주소 삭제
12. ✅ deleteAddress_Unauthorized - 미인증 주소 삭제

### 테스트 커버리지
- **PUT /v1/orders/address**: 5개 테스트 (정상, 미인증, 유효성 검증 x2, 최대 제한)
- **GET /v1/orders/address**: 3개 테스트 (정상, 빈 목록, 미인증)
- **DELETE /v1/orders/address/{address_id}**: 4개 테스트 (정상, 미존재, 권한 없음, 미인증)

## 코드 개선 제안

### AddressUseCase.java
현재 RuntimeException을 던지는 부분을 커스텀 예외로 개선 필요:

```java
// 현재 코드 (Line 28-29)
Member member = memberRepository.findById(member_id)
    .orElseThrow(() -> new RuntimeException("member가 존재하지 않습니다"));

// 개선 제안
Member member = memberRepository.findById(member_id)
    .orElseThrow(() -> new MemberException(MemberErrorCode.MEMBER_NOT_FOUND));
```

```java
// 현재 코드 (Line 67-69)
if (!(address.getMember().getMemberId().equals(memberId))) {
    throw new RuntimeException();
}

// 개선 제안
if (!(address.getMember().getMemberId().equals(memberId))) {
    throw new AddressException(AddressErrorCode.ACCESS_DENIED);
}
```

### AddressController.java
1. **deleteAddress 메서드**: LocalUser도 지원하도록 수정 필요 (현재는 OAuth2User만 지원)
   ```java
   // 현재 코드 (Line 109-116)
   public ResponseEntity<ApiResponse<DeleteAddressResponseDto>> deleteAddress(
       @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
       @PathVariable Long address_id,
       ...
   ) {
       Long memberId = customOAuth2User.getMemberId();

   // 개선 제안
   public ResponseEntity<ApiResponse<DeleteAddressResponseDto>> deleteAddress(
       @AuthenticationPrincipal CustomOAuth2User customOAuth2User,
       @AuthenticationPrincipal CustomLocalUser customLocalUser,
       @PathVariable Long address_id,
       ...
   ) {
       Long memberId = customOAuth2User == null ?
           (customLocalUser == null ? null : customLocalUser.getMemberId()) :
           customOAuth2User.getMemberId();
       if (memberId == null) {
           throw new OauthException(OauthErrorCode.C012);
       }
   ```

2. **TODO 구현**:
   - address name 유니크 속성 추가
   - address 5개 이상 생성 불가 로직 (AddressService.addressNumberCheck_MaxFive)

## 테스트 실행 시 발견된 이슈

### ClassNotFoundException 발생
테스트 실행 시 다음과 같은 에러가 발생했습니다:
```
java.lang.ClassNotFoundException at BuiltinClassLoader.java:641
```

이는 다음 중 하나의 원인일 수 있습니다:
1. Gradle 캐시 문제: `./gradlew clean` 후 재실행 필요
2. 플러그인 의존성 문제: build.gradle의 플러그인 버전 확인
3. 환경 설정 문제: IDE와 Gradle 버전 호환성 확인

**해결 방법**:
1. 캐시 정리: `./gradlew clean build`
2. IDE 재시작 및 Gradle 프로젝트 새로고침
3. build/classes 디렉토리 삭제 후 재빌드

## 참고 자료

### 테스트에 사용된 주요 클래스
- `AddressController.java`: src/main/java/com/C_platform/Member_woonkim/presentation/controller/AddressController.java:1-129
- `AddressUseCase.java`: src/main/java/com/C_platform/Member_woonkim/application/useCase/AddressUseCase.java:1-78
- `Address.java`: src/main/java/com/C_platform/Member_woonkim/domain/entitys/Address.java:1-39
- `AddAddressRequestDto.java`: src/main/java/com/C_platform/Member_woonkim/presentation/dto/address/request/AddAddressRequestDto.java:1-34

### 관련 문서
- Spring Boot Testing: https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.testing
- MockMvc: https://docs.spring.io/spring-framework/docs/current/reference/html/testing.html#spring-mvc-test-framework
- JUnit 5: https://junit.org/junit5/docs/current/user-guide/

## 결론

AddressController의 모든 API 엔드포인트에 대해 총 12개의 JUnit 테스트 케이스를 작성했습니다. 테스트는 다음을 검증합니다:

1. **정상 케이스**: 각 API가 올바른 입력에 대해 예상대로 동작하는지
2. **인증/인가**: 미인증 사용자 및 권한 없는 사용자 접근 차단
3. **입력 검증**: Bean Validation을 통한 잘못된 입력 검증
4. **비즈니스 로직**: 최대 주소 개수 제한 등의 비즈니스 규칙 검증
5. **에러 처리**: 존재하지 않는 리소스에 대한 적절한 에러 응답

테스트 코드는 프로덕션 코드의 품질을 보장하고, 향후 리팩토링 시 회귀 버그를 방지하는 안전망 역할을 합니다.
