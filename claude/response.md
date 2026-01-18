# AddressControllerTest 실패 원인 분석 (2026-01-18)

## 테스트 결과 요약

```
12 tests completed, 3 failed (9개 통과)
```

**참고:** 요청에서는 "모든 케이스가 실패"라고 하셨지만, 실제로는 **9개 통과, 3개 실패**입니다.

---

## 실패한 테스트 분석

### 1. deleteAddress_NotFound (Line 327)

**에러:**
```
NullPointerException: Cannot invoke "Address.getMember()" because "address" is null
    at AddressUseCase.deleteAddress(AddressUseCase.java:67)
```

**원인 코드:** `AddressUseCase.java:62-67`

```java
@Transactional
public void deleteAddress(Long memberId, Long addressId) {
    Address address = addressRepository.findAddressByAddressId(addressId);

    // TODO : 에러 만들기
    if (!(address.getMember().getMemberId().equals(memberId))) {  // ← NPE 발생!
        throw new RuntimeException();
    }
    // ...
}
```

**문제:**
- `findAddressByAddressId(99999L)`가 존재하지 않는 주소 ID를 조회하면 `null` 반환
- `null.getMember()` 호출 시 `NullPointerException` 발생

**해결 방법:**
```java
public void deleteAddress(Long memberId, Long addressId) {
    Address address = addressRepository.findAddressByAddressId(addressId);

    // null 체크 추가
    if (address == null) {
        throw new AddressException(AddressErrorCode.C002, "주소를 찾을 수 없습니다");
    }

    if (!(address.getMember().getMemberId().equals(memberId))) {
        throw new AddressException(AddressErrorCode.C003, "본인의 주소만 삭제할 수 있습니다");
    }
    // ...
}
```

---

### 2. deleteAddress_Forbidden (Line 362)

**에러:**
```
ServletException
    Caused by: RuntimeException
```

**원인 코드:** `AddressUseCase.java:67-68`

```java
if (!(address.getMember().getMemberId().equals(memberId))) {
    throw new RuntimeException();  // ← 메시지 없는 예외
}
```

**문제:**
- 다른 사용자의 주소를 삭제하려 할 때 단순 `RuntimeException()` 발생
- 테스트는 `status().is5xxServerError()` 기대
- 예외 처리기가 `RuntimeException`을 적절히 처리하지 못할 수 있음

**테스트 코드:**
```java
// member_id=2의 주소 생성
Address otherAddress = addressUseCase.addAddress(OTHER_MEMBER_ID, dto);

// member_id=1로 인증하여 삭제 시도
mockMvc.perform(delete("/v1/orders/address/{address_id}", otherAddress.getAddressId())
        .with(authentication(auth)))
    .andExpect(status().is5xxServerError());  // ← 기대값
```

**해결 방법:**
```java
if (!(address.getMember().getMemberId().equals(memberId))) {
    throw new AddressException(AddressErrorCode.C003, "본인의 주소만 삭제할 수 있습니다");
}
```

---

### 3. addAddress_MaxLimitExceeded (Line 419)

**에러:**
```
AssertionError at AddressControllerTest.java:419
```

**테스트 기대값:**
```java
.andExpect(status().is4xxClientError());  // 4xx 기대
```

**실제 발생 예외:** `AddressService.java:13-18`

```java
public void addressNumberCheck_MaxFive(List<Address> addressList) {
    final int MAX = 5;
    if (addressList != null && addressList.size() >= MAX) {
        throw new AddressException(AddressErrorCode.C001,
            "주소는 최대 " + MAX + "개까지만 등록 가능합니다. (현재: " + addressList.size() + ")");
    }
}
```

**문제:**
- `AddressException`이 발생하지만, 응답 코드가 4xx가 아닐 수 있음
- `@ControllerAdvice` 또는 예외 처리기에서 `AddressException`을 어떤 HTTP 상태로 변환하는지 확인 필요
- 현재 500 (Internal Server Error)로 응답할 가능성 높음

**해결 방법:**
1. `AddressException` 예외 처리기 확인 및 수정
2. 또는 테스트 기대값을 수정:
   ```java
   .andExpect(status().is5xxServerError());  // 또는
   .andExpect(status().isBadRequest());       // 400
   ```

---

## 핵심 문제 요약

| 테스트 | 에러 | 근본 원인 |
|--------|------|-----------|
| deleteAddress_NotFound | NullPointerException | `findAddressByAddressId()` null 반환 후 체크 없이 사용 |
| deleteAddress_Forbidden | RuntimeException | 메시지 없는 예외, 적절한 ErrorCode 미사용 |
| addAddress_MaxLimitExceeded | AssertionError | AddressException → HTTP 상태 코드 매핑 문제 |

---

## AddressUseCase 개선 제안

```java
@Transactional
public void deleteAddress(Long memberId, Long addressId) {
    Address address = addressRepository.findAddressByAddressId(addressId);

    // 1. null 체크 추가
    if (address == null) {
        throw new AddressException(AddressErrorCode.C002, "주소를 찾을 수 없습니다");
    }

    // 2. 적절한 예외로 변경
    if (!address.getMember().getMemberId().equals(memberId)) {
        throw new AddressException(AddressErrorCode.C003, "본인의 주소만 삭제할 수 있습니다");
    }

    addressRepository.delete(address);
}
```

---

## 통과한 테스트 (9개)

1. addAddress_Success ✅
2. addAddress_Unauthorized ✅
3. addAddress_InvalidInput_BlankAddressName ✅
4. addAddress_InvalidInput_MissingZipCode ✅
5. getAddressList_Success ✅
6. getAddressList_EmptyList ✅
7. getAddressList_Unauthorized ✅
8. deleteAddress_Success ✅
9. deleteAddress_Unauthorized ✅

---

## 작성일

2026-01-18

## 작성자

Claude Code
