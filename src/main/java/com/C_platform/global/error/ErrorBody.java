package com.C_platform.global.error;

import com.C_platform.global.Detail;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class ErrorBody<T extends ErrorCode> {
    private String code;
    private String message;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private ArrayList<Detail> details;

    public ErrorBody(T code) {
        this.code = code.getCode();
        this.message = code.getMessage();
    }

    public ErrorBody(T code, ArrayList<Detail> details) {
        this.code = code.getCode();
        this.message = code.getMessage();
        this.details = details;
    }
}
