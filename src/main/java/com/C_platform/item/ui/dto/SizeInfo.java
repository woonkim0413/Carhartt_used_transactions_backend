package com.C_platform.item.ui.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SizeInfo {
    @JsonProperty("total_length")
    private int totalLength;
    private int sleeve;
    private int shoulder;
    private int chest;
}
