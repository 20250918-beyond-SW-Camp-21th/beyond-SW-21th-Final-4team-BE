package com.fallguys.email.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerifyCodeRequestDto {

    private String email;
    private String code;

    public EmailVerifyCodeRequestDto(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
