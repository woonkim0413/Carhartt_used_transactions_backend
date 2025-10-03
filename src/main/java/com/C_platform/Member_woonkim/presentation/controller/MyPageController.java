package com.C_platform.Member_woonkim.presentation.controller;

import com.C_platform.Member_woonkim.application.useCase.MyPageUseCase;
import com.C_platform.Member_woonkim.domain.Oauth.CustomOAuth2User;
import com.C_platform.Member_woonkim.presentation.dto.ChangeNicknameRequestDto;
import com.C_platform.global.ApiResponse;
import com.C_platform.global.MetaData;
import com.C_platform.global.logPaint;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("v1/")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageUseCase myPageUseCase;

    @PostMapping("/myPage/change_nickname")
    @Operation(summary = "사용자 닉네임 변경", description = "마이 페이지에서 사용자 닉네임을 변경할 때 사용합니다")
    public ResponseEntity<ApiResponse<Map<String, String>>> changeNickname(
            @Valid @RequestBody ChangeNicknameRequestDto dto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ) {
        logPaint.sep("changeNickname 진입");
        String changeNickname = dto.changeNickname();
        Long memberId = customOAuth2User.getMemberId();

        // todo : UseCase Annotation 생성
        // todo : 이름 변경 전용 에러 코드 설게
        myPageUseCase.changeMemberNickname(changeNickname, memberId);

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("memberId", memberId.toString());
        responseBody.put("nickname", changeNickname);

        // todo : meta에 실리는 로깅 값 넣도록 변경
        MetaData meta = MetaData.builder()
                .timestamp(LocalDateTime.now())
                .build();

        logPaint.sep("changeNickname 이탈");
        return ResponseEntity.ok(ApiResponse.success(responseBody, meta));
    }
}
