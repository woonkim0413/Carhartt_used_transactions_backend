package com.C_platform.Member_woonkim.presentation.dto.Local.response;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SuccessMessageResponseDto(
        @JsonProperty("success_message")
        String successMessage
) {}
