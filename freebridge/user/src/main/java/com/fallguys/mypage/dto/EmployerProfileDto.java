package com.fallguys.mypage.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class EmployerProfileDto {
    private String companyName;
    private String businessRegistrationNumber;
    private String contactEmail;
    private String description;
    private String logoUrl;

    public EmployerProfileDto(String companyName, String businessRegistrationNumber, String contactEmail, String description, String logoUrl) {
        this.companyName = companyName;
        this.businessRegistrationNumber = businessRegistrationNumber;
        this.contactEmail = contactEmail;
        this.description = description;
        this.logoUrl = logoUrl;
    }
}
