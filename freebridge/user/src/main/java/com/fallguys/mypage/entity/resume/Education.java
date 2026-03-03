package com.fallguys.mypage.entity.resume;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    private String schoolType;
    private String schoolName;
    private String major;
    
    @Enumerated(EnumType.STRING)
    private EduStatus eduStatus;
    
    private LocalDate entranceDate;
    private LocalDate graduationDate;
}
