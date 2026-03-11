package com.fallguys.user.api.web.dto.request;

import lombok.Getter;
import java.util.Optional;

@Getter
public class AccountInfoUpdateRequest {

    private String name;
    private Optional<String> phone;
}
