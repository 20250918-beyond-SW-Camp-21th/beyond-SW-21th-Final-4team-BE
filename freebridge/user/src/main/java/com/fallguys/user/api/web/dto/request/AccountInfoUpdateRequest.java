package com.fallguys.user.api.web.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class AccountInfoUpdateRequest {

    @Pattern(regexp = ".*\\S.*", message = "name must contain at least one non-whitespace character")
    private String name;

    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "phone must be a valid E.164 number")
    private String phone;
}
