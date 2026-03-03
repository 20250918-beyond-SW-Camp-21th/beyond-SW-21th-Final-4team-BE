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
@Table(name = "resume")
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

    public void update(String name, LocalDate birthDate, String phone, String email, String address,
                       List<Education> educations, List<Career> careers, List<Certification> certifications) {
        this.name = name;
        this.birthDate = birthDate;
        this.phone = phone;
        this.email = email;
        this.address = address;

        this.educations.clear();
        if (educations != null) {
            this.educations.addAll(educations);
        }
        this.careers.clear();
        if (careers != null) {
            this.careers.addAll(careers);
        }
        this.certifications.clear();
        if (certifications != null) {
            this.certifications.addAll(certifications);
        }
    }
}
