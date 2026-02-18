package com.fallguys.mypage.entity.freelancer;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "freelancer")
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class Freelancer {

    @Id
    @Column(name = "freelancer_id")
    private Long freelancerId;

    private Long userId;

    private Long resumeId;

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
    private Collaboration collaboration;

    private Double averateRate;
    private Integer statContact;
    private Integer statChat;
    private Integer statContract;
    private Integer topPercentile;

    @Embedded
    private PortfolioInfo portfolioInfo;

    @CreatedDate
    private LocalDateTime createdDate;

    @LastModifiedDate
    private LocalDateTime updatedAt;


    /* --------------------------------
    *   초기화
    *  */

    public static Freelancer create(
            Long userId,
            String job,
            FreelancerGrade grade
    ) {
        if (userId == null) throw new IllegalArgumentException("userId is required");
        if (job == null || job.isBlank()) throw new IllegalArgumentException("job is required");
        if (grade == null) grade = FreelancerGrade.JUNIOR;

        Freelancer f = new Freelancer();
        f.userId = userId;
        f.job = job.trim();
        f.grade = grade;

        // 기본값
        f.skills = new ArrayList<>();
        f.statContact = 0;
        f.statChat = 0;
        f.statContract = 0;
        f.averateRate = 0.0;
        f.topPercentile = 0;
        return f;
    }

    /* --------------------------------
    *   프로필 수정 함수
    * */

    public void updateBasicProfile(String job, String avatarUrl, String introduction) {
        if (job != null && !job.isBlank()) this.job = job.trim();
        if (avatarUrl != null && !avatarUrl.isBlank()) this.avatarUrl = avatarUrl.trim();
        if (introduction != null) this.introduction = introduction; // 길이 제한은 컨트롤러/검증에서 추가 가능
    }

    public void updateCareer(Integer careerYears, Long wage) {
        if (careerYears != null && careerYears < 0) {
            throw new IllegalArgumentException("careerYears must be >= 0");
        }
        if (wage != null && wage < 0) {
            throw new IllegalArgumentException("wage must be >= 0");
        }
        if (careerYears != null) this.careerYears = careerYears;
        if (wage != null) this.wage = wage;
    }

    public void changeGrade(FreelancerGrade newGrade) {
        if (newGrade == null) throw new IllegalArgumentException("grade is required");
        this.grade = newGrade;
    }

    public void connectResume(Long resumeId) {
        if (resumeId == null) throw new IllegalArgumentException("resumeId is required");
        this.resumeId = resumeId;
    }

    public void disconnectResume() {
        this.resumeId = null;
    }

    public void updateWorkConditions(WorkConditions workConditions) {
        if (workConditions == null) throw new IllegalArgumentException("workConditions is required");
        this.workConditions = workConditions;
    }

    public void updateExpertise(Expertise expertise) {
        if (expertise == null) throw new IllegalArgumentException("expertise is required");
        this.expertise = expertise;
    }

    public void updateCollaboration(Collaboration collaboration) {
        if (collaboration == null) throw new IllegalArgumentException("collaboration is required");
        this.collaboration = collaboration;
    }

    public void updatePortfolioInfo(PortfolioInfo portfolioInfo) {
        if (portfolioInfo == null) throw new IllegalArgumentException("portfolioInfo is required");
        this.portfolioInfo = portfolioInfo;
    }

    /* --------------------------------
    *   스킬 관리
    *  */

    public void addSkill(String skill) {
        String normalized = normalizeSkill(skill);
        if (normalized == null) return;
        if (this.skills == null) this.skills = new ArrayList<>();
        if (!this.skills.contains(normalized)) this.skills.add(normalized);
    }

    public void removeSkill(String skill) {
        String normalized = normalizeSkill(skill);
        if (normalized == null || this.skills == null) return;
        this.skills.remove(normalized);
    }

    public void replaceSkills(List<String> newSkills) {
        if (newSkills == null) {
            this.skills = new ArrayList<>();
            return;
        }
        List<String> cleaned = new ArrayList<>();
        for (String s : newSkills) {
            String n = normalizeSkill(s);
            if (n != null && !cleaned.contains(n)) cleaned.add(n);
        }
        this.skills = cleaned;
    }

    private String normalizeSkill(String skill) {
        if (skill == null) return null;
        String s = skill.trim();
        if (s.isBlank()) return null;
        return s;
    }

    /* --------------------------------
    *   통계 관리
    * */

    public void increaseContact() {
        this.statContact = safePlus(this.statContact, 1);
    }

    public void increaseChat() {
        this.statChat = safePlus(this.statChat, 1);
    }

    public void increaseContract() {
        this.statContract = safePlus(this.statContract, 1);
    }

    private int safePlus(Integer v, int delta) {
        return (v == null ? 0 : v) + delta;
    }

    private int safeMinus(Integer v, int delta) {
        int cur = (v == null ? 0 : v);
        int next = cur - delta;
        return Math.max(next, 0);
    }

    /* --------------------------------
       평가/퍼센타일 계산 관련
    */

    public void updateAverageRate(Double averageRate) {
        if (averageRate == null) return;
        if (averageRate < 0.0 || averageRate > 5.0) {
            throw new IllegalArgumentException("averageRate must be between 0.0 and 5.0");
        }
        this.averateRate = averageRate;
    }

    public void updateTopPercentile(Integer topPercentile) {
        if (topPercentile == null) return;
        if (topPercentile < 0 || topPercentile > 100) {
            throw new IllegalArgumentException("topPercentile must be between 0 and 100");
        }
        this.topPercentile = topPercentile;
    }
}
