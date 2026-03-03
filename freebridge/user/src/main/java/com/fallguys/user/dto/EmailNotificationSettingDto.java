package com.fallguys.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class EmailNotificationSettingDto {

    @NotNull(message = "이메일 알림 설정 값을 포함해주세요.")
    private Boolean emailEnabled;
}
