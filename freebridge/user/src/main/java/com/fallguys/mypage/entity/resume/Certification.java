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
public class Certification {

    private String name;
    private String issuer;
    private LocalDate acquisitionDate;
}
