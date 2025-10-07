package com.C_platform.Member_woonkim.utils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LogPaint {
    private static final String RESET   = "\u001B[0m";
    private static final String MAGENTA = "\u001B[1;95m";

    public static void sep(String title) {
        log.info("{}================================================== {} ========================================================={}", MAGENTA, title, RESET);
    }
}
