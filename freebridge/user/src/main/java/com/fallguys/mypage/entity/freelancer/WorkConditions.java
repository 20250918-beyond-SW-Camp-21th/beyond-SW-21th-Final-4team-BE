package com.fallguys.mypage.entity.freelancer;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class WorkConditions {
    private String conditionsType;
    private LocalDate startDate;
    private String workStyle;
    private String location;
}