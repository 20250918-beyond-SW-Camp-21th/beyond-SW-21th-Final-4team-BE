package com.fallguys.email.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerificationRequestDto {

    private String email;

    public EmailVerificationRequestDto(String email) {
        this.email = email;
    }
}
