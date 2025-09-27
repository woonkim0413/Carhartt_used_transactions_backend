package com.C_platform.item.ui.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
@Schema(description = "사이즈 정보")
public class Sizes {

    @Schema(description = "총장", example = "70")
    @JsonProperty("totlal_length")
    @Positive(message = "{item.size.positive.total}")
    private int tottalength;

    @Schema(description = "소매길이", example = "60")
    @Positive(message = "{item.size.positive.sleeve}")
    private int sleeve;

    @Schema(description = "어깨", example = "40")
    @Positive(message = "{item.size.positive.shoulder}")
    private int shoulder;

    @Schema(description = "가슴단면", example = "50")
    @Positive(message = "{item.size.positive.chest}")
    private int chest;

//    @Schema(description = "밑위", example = "30")
//    @Positive(message = "{item.size.positive.rise}")
//    @JsonProperty("rise_length")
//    @JsonIgnore
//    private int riseLength;
//
//    @Schema(description = "허벅지", example = "25")
//    @Positive(message = "{item.size.positive.thigh}")
//    @JsonIgnore
//    private int thigh;
//
//    @Schema(description = "밑단", example = "20")
//    @Positive(message = "{item.size.positive.hem}")
//    @JsonIgnore
//    private int hem;
}
