package com.fallguys.mypage.entity.freelancer;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Collaboration {
    private Integer communication;
    private Integer scheduleAdherence;
    private Integer dispute;
}