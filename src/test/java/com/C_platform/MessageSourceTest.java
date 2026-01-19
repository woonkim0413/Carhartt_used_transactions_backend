package com.C_platform;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;


@SpringBootTest
public class MessageSourceTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    @DisplayName("메시지 소스 테스트")
    @Disabled
    void messageSourceTest() {
        String message = messageSource.getMessage("test.message", null, Locale.KOREA);
        System.out.println("Test Message: " + message);
        assertThat(message).isEqualTo("Hello, World!");
    }
}
