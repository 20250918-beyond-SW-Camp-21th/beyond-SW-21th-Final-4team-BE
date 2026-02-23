package com.fallguys.user.dto;

import com.fallguys.user.entity.Role;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SignupRequestDto {

    private String email;
    private String password;
    private String name;
    private Role role;
    private Boolean termsAgreed;
    private Boolean privacyAgreed;
}
