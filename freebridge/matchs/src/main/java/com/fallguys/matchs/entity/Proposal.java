package com.fallguys.matchs.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

//기업->프리랜서
@Entity
@Table(name="Proposal")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Proposal {

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    @Column(nullable=false)
    private Long jobPostingId;

    @Column(nullable=false)
    private String freelancerId;

    @Column(nullable=false)
    private String employerId;

    @Column(nullable=false)
    private String message;

    @Column(nullable=false)
    @Builder.Default
    private MatchsStatus status=MatchsStatus.PENDING;

    private LocalDateTime createdAt;

}
