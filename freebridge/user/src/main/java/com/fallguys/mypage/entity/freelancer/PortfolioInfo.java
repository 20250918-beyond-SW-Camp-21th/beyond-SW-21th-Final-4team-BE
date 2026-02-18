package com.fallguys.mypage.entity.freelancer;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioInfo {
    private String portfolioFileUrl;
    private String portfolioFileName;
    private String portfolioLastUpdated;
}