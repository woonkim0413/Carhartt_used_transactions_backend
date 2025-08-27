package com.C_platform.global;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class ErrorBody {
    private String code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ArrayList<Detail> details;

    public ErrorBody(ProductErrorCode code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public ErrorBody(ProductErrorCode code, ArrayList<Detail> details) {
        this.code = code.getCode();
        this.message = code.getMessage();
        this.details = details;

    }
}
