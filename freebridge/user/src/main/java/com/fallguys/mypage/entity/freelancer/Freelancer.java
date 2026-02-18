package com.fallguys.mypage.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@Table(name = "freelancer")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class Freelancer {

    @Id
    @Column(name = "freelancer_id")
    private UUID id;

    private Long userId;

    private FreelancerGrade grade;

    private String job;

    private String avatarUrl;

    @Column(columnDefinition = "TEXT")
    private String introduction;

    private Integer careerYears;

    private Long wage; // 시급

    @Embedded
    private WorkConditions workConditions;

    @ElementCollection
    @CollectionTable(name = "freelancer_skills", joinColumns = @JoinColumn(name = "freelancer_id"))
    @Column(name = "skill")
    private List<String> skills = new ArrayList<>();

    @Embedded
    private Expertise expertise;

    @Embedded
    FreelancerProfile.Collaboration collaboration;

    private Double averateRate;
    private Integer statContact;
    private Integer statChat;
    private Integer statContract;
    private Integer statInteresting;
    private Integer topPercentile;

    @Embeddable
    @Getter @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkConditions {
        private String conditionsType;
        private String startDate;
        private String workStyle;
        private String location;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Expertise {
        private Integer programming;
        private Integer framework;
        private Integer problemSolving;
    }

    @Embeddable
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Collaboration {
        private Integer communication;
        private Integer scheduleAdherence;
        private Integer dispute;
    }
}
