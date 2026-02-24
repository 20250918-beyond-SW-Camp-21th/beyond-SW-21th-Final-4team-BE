package com.fallguys.mypage.entity.resume;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Career {

    private String companyName;
    private String department;
    private String position;
    private String jobType;
    private String employmentType;
    private LocalDate startDate;
    private LocalDate endDate;
    private String description;
}
