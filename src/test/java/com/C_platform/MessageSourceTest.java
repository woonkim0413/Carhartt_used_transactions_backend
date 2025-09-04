package com.C_platform;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
@Slf4j
public class MessageSourceTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    @DisplayName("메시지 소스 테스트")
    void messageSourceTest() {
        String message = messageSource.getMessage("test.message", null, Locale.KOREA);
        log.info("Test Message: {}", message);
        assertThat(message).isEqualTo("Hello, World!");
    }
}
