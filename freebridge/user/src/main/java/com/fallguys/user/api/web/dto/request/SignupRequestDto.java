package com.fallguys.user.api.web.dto.request;

import com.fallguys.user.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {

    @NotBlank(message = "이메일은 필수입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    private String email;

    @NotBlank(message = "비밀번호는 필수입니다.")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$", message = "비밀번호는 대문자, 소문자, 숫자, 특수문자를 포함하여 8자 이상이어야 합니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String phone;

    @NotNull(message = "역할은 필수입니다.")
    private Role role;

    @NotNull(message = "이용약관 동의 여부는 필수입니다.")
    private Boolean termsAgreed;

    @NotNull(message = "개인정보처리방침 동의 여부는 필수입니다.")
    private Boolean privacyAgreed;
}
