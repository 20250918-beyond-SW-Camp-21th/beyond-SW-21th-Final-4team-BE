package com.fallguys.email.api.web.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class EmailVerifyCodeRequestDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "인증코드는 필수입니다.")
    private String code;

    public EmailVerifyCodeRequestDto(String email, String code) {
        this.email = email;
        this.code = code;
    }
}
