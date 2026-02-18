package com.fallguys.mypage.entity.resume;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Education {

    private String schoolType;
    private String schoolName;
    private String major;
    private String status;
    private String entranceDate;
    private String graduationDate;
}
