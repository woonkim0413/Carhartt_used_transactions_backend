package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.AddressUseCase;
import com.C_platform.Member_woonkim.domain.entitys.Address;
import com.C_platform.Member_woonkim.domain.entitys.Member;
import com.C_platform.Member_woonkim.domain.enums.LocalProvider;
import com.C_platform.Member_woonkim.domain.value.CustomOAuth2User;
import com.C_platform.Member_woonkim.infrastructure.db.AddressRepository;
import com.C_platform.Member_woonkim.infrastructure.db.MemberRepository;
import com.C_platform.Member_woonkim.presentation.dto.address.request.AddAddressRequestDto;
import com.C_platform.annotation.WithMockCustomUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AddressController의 통합 테스트 클래스
 * - PUT /v1/orders/address: 주소 추가
 * - GET /v1/orders/address: 주소 목록 조회
 * - DELETE /v1/orders/address/{address_id}: 주소 삭제
 */
@SpringBootTest(
        properties = {
                "spring.session.store-type=none",
                "spring.data.redis.repositories.enabled=false",
                "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
        }
)
@AutoConfigureMockMvc // Mcok을 field에 주입
@Transactional // 테스트 코드 동작 후 db 초기화
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private AddressUseCase addressUseCase;

    private Member testMember;

    @BeforeEach
    void setUp() {
        // 테스트용 회원 생성 및 저장
        testMember = new Member(
                LocalProvider.LOCAL,
                "dnsrkd0414@naver.com",
                "encodedPassword123",
                "김운강"
        );
        memberRepository.save(testMember);
    }

    /**
     * 테스트 1: 주소 추가 API - 정상 케이스
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강",
            provider = LocalProvider.LOCAL
    )
    @DisplayName("정상적인 주소 추가 요청 시 주소가 생성되고 200 OK 반환")
    void addAddress_Success() throws Exception {
        // given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("address_name", "본가");
        requestBody.put("zip_code", "12345");
        requestBody.put("road_address", "서울시 강남구 테헤란로 123");
        requestBody.put("detail_address", "101동 101호");

        // when & then
        mockMvc.perform(put("/v1/orders/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("X-Request-Id", "test-req-001")
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(testMember.getMemberId().toString()))
                .andExpect(jsonPath("$.data.addressId").exists())
                .andExpect(jsonPath("$.data.addressName").value("본가"))
                .andExpect(jsonPath("$.meta.x_request_id").value("test-req-001"));

        // 검증: DB에 주소가 실제로 저장되었는지 확인
        List<Address> savedAddresses = addressRepository.findAllByMember_MemberId(testMember.getMemberId());
        assertThat(savedAddresses).hasSize(1);
        assertThat(savedAddresses.get(0).getAddressName()).isEqualTo("본가");
    }

    /**
     * 테스트 2: 주소 추가 API - 인증되지 않은 사용자
     */
    @Test
    @DisplayName("인증되지 않은 사용자의 주소 추가 요청 시 401 반환")
    void addAddress_Unauthorized() throws Exception {
        // given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("address_name", "본가");
        requestBody.put("zip_code", "12345");
        requestBody.put("road_address", "서울시 강남구 테헤란로 123");
        requestBody.put("detail_address", "101동 101호");

        // when & then: @WithMockCustomUser 없음 = 인증 없음
        mockMvc.perform(put("/v1/orders/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트 3: 주소 추가 API - 유효하지 않은 입력 (빈 주소 이름)
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("주소 이름이 빈 값일 경우 400 Bad Request 반환")
    void addAddress_InvalidInput_BlankAddressName() throws Exception {
        // given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("address_name", ""); // 빈 값
        requestBody.put("zip_code", "12345");
        requestBody.put("road_address", "서울시 강남구 테헤란로 123");
        requestBody.put("detail_address", "101동 101호");

        // when & then
        mockMvc.perform(put("/v1/orders/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * 테스트 4: 주소 추가 API - 유효하지 않은 입력 (우편번호 누락)
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("우편번호가 누락된 경우 400 Bad Request 반환")
    void addAddress_InvalidInput_MissingZipCode() throws Exception {
        // given
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("address_name", "본가");
        // zip_code 누락
        requestBody.put("road_address", "서울시 강남구 테헤란로 123");
        requestBody.put("detail_address", "101동 101호");

        // when & then
        mockMvc.perform(put("/v1/orders/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    /**
     * 테스트 5: 주소 목록 조회 API - 정상 케이스
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("정상적인 주소 목록 조회 요청 시 주소 목록과 200 OK 반환")
    void getAddressList_Success() throws Exception {
        // given: 테스트용 주소 2개 추가
        AddAddressRequestDto dto1 = new AddAddressRequestDto(
                "본가", "12345", "서울시 강남구 테헤란로 123", "101동 101호"
        );
        AddAddressRequestDto dto2 = new AddAddressRequestDto(
                "회사", "67890", "서울시 서초구 강남대로 456", "202동 202호"
        );
        addressUseCase.addAddress(testMember.getMemberId(), dto1);
        addressUseCase.addAddress(testMember.getMemberId(), dto2);

        // when & then
        mockMvc.perform(get("/v1/orders/address")
                        .header("X-Request-Id", "test-req-002"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.memberId").value(testMember.getMemberId().toString()))
                .andExpect(jsonPath("$.data.addressList").isArray())
                .andExpect(jsonPath("$.data.addressList.length()").value(2))
                .andExpect(jsonPath("$.meta.x_request_id").value("test-req-002"));
    }

    /**
     * 테스트 6: 주소 목록 조회 API - 주소가 없는 경우
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("주소가 없는 사용자의 주소 목록 조회 시 빈 배열과 200 OK 반환")
    void getAddressList_EmptyList() throws Exception {
        // given: 주소를 추가하지 않음

        // when & then
        mockMvc.perform(get("/v1/orders/address"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.addressList").isArray())
                .andExpect(jsonPath("$.data.addressList.length()").value(0));
    }

    /**
     * 테스트 7: 주소 목록 조회 API - 인증되지 않은 사용자
     */
    @Test
    @DisplayName("인증되지 않은 사용자의 주소 목록 조회 요청 시 401 반환")
    void getAddressList_Unauthorized() throws Exception {
        // when & then: @WithMockCustomUser 없음 = 인증 없음
        mockMvc.perform(get("/v1/orders/address"))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트 8: 주소 삭제 API - 정상 케이스
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("정상적인 주소 삭제 요청 시 주소가 삭제되고 200 OK 반환")
    void deleteAddress_Success() throws Exception {
        // given: 테스트용 주소 추가
        AddAddressRequestDto dto = new AddAddressRequestDto(
                "본가", "12345", "서울시 강남구 테헤란로 123", "101동 101호"
        );
        Address savedAddress = addressUseCase.addAddress(testMember.getMemberId(), dto);
        Long addressId = savedAddress.getAddressId();

        // Mock CustomOAuth2User 생성 (deleteAddress는 OAuth2User만 지원)
        CustomOAuth2User mockOAuth2User = new CustomOAuth2User(
                testMember.getMemberId(),
                Map.of("name", testMember.getName(), "email", testMember.getEmail()),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockOAuth2User, null, mockOAuth2User.getAuthorities());

        // when & then
        mockMvc.perform(delete("/v1/orders/address/{address_id}", addressId)
                        .with(authentication(auth))
                        .header("X-Request-Id", "test-req-003"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.meta.x_request_id").value("test-req-003"));

        // 검증: DB에서 주소가 실제로 삭제되었는지 확인
        List<Address> remainingAddresses = addressRepository.findAllByMember_MemberId(testMember.getMemberId());
        assertThat(remainingAddresses).isEmpty();
    }

    /**
     * 테스트 9: 주소 삭제 API - 존재하지 않는 주소 ID
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("존재하지 않는 주소 ID로 삭제 요청 시 500 또는 400 반환")
    void deleteAddress_NotFound() throws Exception {
        // given
        Long nonExistentAddressId = 99999L;

        CustomOAuth2User mockOAuth2User = new CustomOAuth2User(
                testMember.getMemberId(),
                Map.of("name", testMember.getName(), "email", testMember.getEmail()),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockOAuth2User, null, mockOAuth2User.getAuthorities());

        // when & then
        mockMvc.perform(delete("/v1/orders/address/{address_id}", nonExistentAddressId)
                        .with(authentication(auth)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    /**
     * 테스트 10: 주소 삭제 API - 다른 사용자의 주소 삭제 시도
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("다른 사용자의 주소 삭제 시도 시 500 또는 403 반환")
    void deleteAddress_Forbidden() throws Exception {
        // given: 다른 사용자와 그 사용자의 주소 생성
        Member otherMember = new Member(
                LocalProvider.LOCAL,
                "other@example.com",
                "encodedPassword456",
                "다른유저"
        );
        memberRepository.save(otherMember);

        AddAddressRequestDto dto = new AddAddressRequestDto(
                "다른유저집", "11111", "부산시 해운대구", "303동 303호"
        );
        Address otherAddress = addressUseCase.addAddress(otherMember.getMemberId(), dto);

        // testMember로 인증 (CustomOAuth2User 사용)
        CustomOAuth2User mockOAuth2User = new CustomOAuth2User(
                testMember.getMemberId(),
                Map.of("name", testMember.getName(), "email", testMember.getEmail()),
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
        );
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(mockOAuth2User, null, mockOAuth2User.getAuthorities());

        // when & then: testMember가 otherMember의 주소 삭제 시도
        mockMvc.perform(delete("/v1/orders/address/{address_id}", otherAddress.getAddressId())
                        .with(authentication(auth)))
                .andDo(print())
                .andExpect(status().is5xxServerError());
    }

    /**
     * 테스트 11: 주소 삭제 API - 인증되지 않은 사용자
     */
    @Test
    @DisplayName("인증되지 않은 사용자의 주소 삭제 요청 시 401 반환")
    void deleteAddress_Unauthorized() throws Exception {
        // given: 테스트용 주소 추가
        AddAddressRequestDto dto = new AddAddressRequestDto(
                "본가", "12345", "서울시 강남구 테헤란로 123", "101동 101호"
        );
        Address savedAddress = addressUseCase.addAddress(testMember.getMemberId(), dto);

        // when & then: @WithMockCustomUser 없음 = 인증 없음
        mockMvc.perform(delete("/v1/orders/address/{address_id}", savedAddress.getAddressId()))
                .andDo(print())
                .andExpect(status().isUnauthorized());
    }

    /**
     * 테스트 12: 주소 추가 API - 최대 5개 제한 검증
     */
    @Test
    @WithMockCustomUser(
            email = "dnsrkd0414@naver.com",
            memberId = 1L,
            name = "김운강"
    )
    @DisplayName("주소가 이미 5개인 경우 추가 시 에러 반환")
    void addAddress_MaxLimitExceeded() throws Exception {
        // given: 주소 5개 추가
        for (int i = 1; i <= 5; i++) {
            AddAddressRequestDto dto = new AddAddressRequestDto(
                    "주소" + i, "1234" + i, "도로명주소" + i, "상세주소" + i
            );
            addressUseCase.addAddress(testMember.getMemberId(), dto);
        }

        // 6번째 주소 추가 시도
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("address_name", "주소6");
        requestBody.put("zip_code", "123456");
        requestBody.put("road_address", "도로명주소6");
        requestBody.put("detail_address", "상세주소6");

        // when & then
        mockMvc.perform(put("/v1/orders/address")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().is4xxClientError());
    }
}