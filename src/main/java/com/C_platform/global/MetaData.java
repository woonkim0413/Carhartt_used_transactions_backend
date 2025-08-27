package com.C_platform.global;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MetaData {
    private String message;

    public MetaData(String message) {
        this.message = message;
    }
}
