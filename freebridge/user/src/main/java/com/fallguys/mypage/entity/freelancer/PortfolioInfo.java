package com.fallguys.mypage.entity.freelancer;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioInfo {
    private String portfolioFileUrl;
    private String portfolioFileName;
    private LocalDateTime portfolioLastUpdated;
}