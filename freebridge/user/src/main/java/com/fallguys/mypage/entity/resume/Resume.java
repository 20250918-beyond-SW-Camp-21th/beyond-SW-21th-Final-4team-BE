package com.fallguys.mypage.entity.resume;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Table(name = "resume", uniqueConstraints = {
        @UniqueConstraint(name = "uq_resume_freelancer_id", columnNames = {"freelancer_id"})
})
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor
public class Resume {

    @Id
    @Column(name = "resume_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long resumeId;

    @Column(name = "freelancer_id", nullable = false, unique = true)
    private Long freelancerId;

    private String name;
    private LocalDate birthDate;
    private String phone;
    private String email;
    private String address;

    @ElementCollection
    @CollectionTable(name = "resume_education", joinColumns = @JoinColumn(name = "resume_id"))
    private List<Education> educations = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "resume_career", joinColumns = @JoinColumn(name = "resume_id"))
    private List<Career> careers = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "resume_certification", joinColumns = @JoinColumn(name = "resume_id"))
    private List<Certification> certifications = new ArrayList<>();

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    public Resume(Long freelancerId) {
        this.freelancerId = freelancerId;
    }

    /** 기본 인적사항 수정 */
    public void updateBasicInfo(String name, LocalDate birthDate, String phone, String email, String address) {
        this.name = name;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.address = address;
    }

    /** 전체 덮어쓰기 (Full Replace) */
    public void update(String name, LocalDate birthDate, String phone, String email, String address,
                       List<Education> educations, List<Career> careers, List<Certification> certifications) {
        updateBasicInfo(name, birthDate, phone, email, address);
        this.educations.clear();
        if (educations != null) {
            educations.stream().filter(java.util.Objects::nonNull).forEach(this.educations::add);
        }
        this.careers.clear();
        if (careers != null) {
            careers.stream().filter(java.util.Objects::nonNull).forEach(this.careers::add);
        }
        this.certifications.clear();
        if (certifications != null) {
            certifications.stream().filter(java.util.Objects::nonNull).forEach(this.certifications::add);
        }
    }

    // ─── 학력 CUD ─────────────────────────────────────────────────

    public void addEducation(Education education) {
        if (education == null) throw new IllegalArgumentException("학력 정보가 없습니다.");
        if (this.educations == null) this.educations = new ArrayList<>();
        this.educations.add(education);
    }

    public void updateEducation(int index, Education education) {
        if (education == null) throw new IllegalArgumentException("학력 정보가 없습니다.");
        validateIndex(index, this.educations, "학력");
        this.educations.set(index, education);
    }

    public void removeEducation(int index) {
        validateIndex(index, this.educations, "학력");
        this.educations.remove(index);
    }

    // ─── 경력 CUD ─────────────────────────────────────────────────

    public void addCareer(Career career) {
        if (career == null) throw new IllegalArgumentException("경력 정보가 없습니다.");
        if (this.careers == null) this.careers = new ArrayList<>();
        this.careers.add(career);
    }

    public void updateCareerEntry(int index, Career career) {
        if (career == null) throw new IllegalArgumentException("경력 정보가 없습니다.");
        validateIndex(index, this.careers, "경력");
        this.careers.set(index, career);
    }

    public void removeCareer(int index) {
        validateIndex(index, this.careers, "경력");
        this.careers.remove(index);
    }

    // ─── 자격증 CUD ───────────────────────────────────────────────

    public void addCertification(Certification certification) {
        if (certification == null) throw new IllegalArgumentException("자격증 정보가 없습니다.");
        if (this.certifications == null) this.certifications = new ArrayList<>();
        this.certifications.add(certification);
    }

    public void updateCertification(int index, Certification certification) {
        if (certification == null) throw new IllegalArgumentException("자격증 정보가 없습니다.");
        validateIndex(index, this.certifications, "자격증");
        this.certifications.set(index, certification);
    }

    public void removeCertification(int index) {
        validateIndex(index, this.certifications, "자격증");
        this.certifications.remove(index);
    }

    // ─── 내부 유효성 검사 ──────────────────────────────────────────

    private void validateIndex(int index, List<?> list, String typeName) {
        if (list == null || index < 0 || index >= list.size()) {
            throw new IllegalArgumentException("유효하지 않은 " + typeName + " 인덱스입니다: " + index);
        }
    }
}
