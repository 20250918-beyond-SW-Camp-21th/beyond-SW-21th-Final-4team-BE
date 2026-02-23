package com.fallguys.mypage.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerProfileDto {
    private String name;
    private String grade;
    private String avatar; // 'avatar' in FE
    private String job;
    private String introduction;
    private Integer careerYears;
    private Long salary;
    private WorkConditionsDto workConditions;
    private List<String> skills;
    private ExpertiseDto expertise;
    private CollaborationDto collaboration;
    
    // Stats
    private Double averageRating;
    private Integer statContact;
    private Integer statChat;
    private Integer statContract;
    private Integer statInteresting;
    private Integer statCompleted;
    private Integer topPercentile; // Optional in FE

    private PortfolioDto portfolio;
    private String resumeUrl; // Separate field

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class WorkConditionsDto {
        private String type; // FE expects 'type'
        private String startDate;
        private String workStyle;
        private String location;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class ExpertiseDto {
        private Integer programming;
        private Integer framework;
        private Integer problemSolving;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CollaborationDto {
        private Integer communication;
        private Integer scheduleAdherence;
        private Integer dispute;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PortfolioDto {
        private String fileUrl; // FE expects 'fileUrl'
        private String fileName;
        private String lastUpdated;
    }
}
