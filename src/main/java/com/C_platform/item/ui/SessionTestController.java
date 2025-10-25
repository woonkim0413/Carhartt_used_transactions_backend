package com.C_platform.item.ui;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;


@Slf4j
@RestController
@RequestMapping("/v1")
@Tag(name = "Session", description = "세션 테스트")
public class SessionTestController {

    @GetMapping("/test/session-check")
    @Operation(summary = "아이템 찜하기", description = "특정 아이템을 회원의 찜 목록에 추가합니다.")
    public Map<String, String> testSession(HttpServletRequest request) {

        // (1) request.getSession(true):
        // 세션이 있으면 "가져오고", 없으면 "새로 생성"합니다.
        HttpSession session = request.getSession(true);
        String sessionId = session.getId();

        // (2) 세션에서 "visitCounter"라는 이름의 속성을 찾습니다.
        Object counterObj = session.getAttribute("user");

        Integer counter;

        if (counterObj == null) {
            // (3) 세션에 "visitCounter"가 없으면 (이 세션에서의 첫 방문)
            counter = 1;
            log.info("[세션 테스트] 새로운 세션입니다. ID: {}", sessionId);
        } else {
            // (4) 세션에 "visitCounter"가 있으면 (기존 방문자)
            counter = (Integer) counterObj;
            counter++; // 카운트를 1 증가시킵니다.
            log.info("[세션 테스트] 기존 세션입니다. ID: {}, Count: {}", sessionId, counter);
        }

        // (5) 증가된 카운터를 세션에 다시 저장합니다.
        session.setAttribute("visitCounter", counter);

        // (6) 프론트엔드로 현재 카운트와 세션 ID를 반환합니다.
        Map<String, String> response = new HashMap<>();
        response.put("sessionId", sessionId);
        response.put("visitCount", String.valueOf(counter));

        return response;
    }
}