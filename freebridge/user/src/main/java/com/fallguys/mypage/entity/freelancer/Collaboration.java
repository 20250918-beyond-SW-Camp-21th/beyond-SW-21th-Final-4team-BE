package com.fallguys.mypage.entity.freelancer;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class Collaboration {
    private Double communication;
    private Double scheduleAdherence;
    private Double dispute;

    public Collaboration(Double communication, Double scheduleAdherence, Double dispute) {
        validateScore(communication, "communication");
        validateScore(scheduleAdherence, "scheduleAdherence");
        validateScore(dispute, "dispute");
        this.communication = communication;
        this.scheduleAdherence = scheduleAdherence;
        this.dispute = dispute;
    }

    public void validate() {
        validateScore(communication, "communication");
        validateScore(scheduleAdherence, "scheduleAdherence");
        validateScore(dispute, "dispute");
    }

    private void validateScore(Double score, String fieldName) {
        if (score == null) {
            return;
        }
        if (score < 0.0 || score > 5.0) {
            throw new IllegalArgumentException(fieldName + " must be between 0.0 and 5.0");
        }
    }
}
