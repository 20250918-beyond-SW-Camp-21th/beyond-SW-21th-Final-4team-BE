package com.fallguys.freebridge.shared.entity;

import com.fallguys.freebridge.shared.enums.EntityStatus;
import jakarta.persistence.*;
import lombok.Getter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    protected EntityStatus status = EntityStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void activate() {
        this.status = EntityStatus.ACTIVE;
    }

    public void delete() {
        if (this.status == EntityStatus.DELETED) {
            return;
        }
        this.status = EntityStatus.DELETED;
    }

    public boolean isActive() {
        return this.status == EntityStatus.ACTIVE;
    }
}
