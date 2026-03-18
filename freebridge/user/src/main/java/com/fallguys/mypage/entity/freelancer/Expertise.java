package com.fallguys.mypage.entity.freelancer;


import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
public class Expertise {
    private Double programming;
    private Double framework;
    private Double problemSolving;

    public Expertise(Double programming, Double framework, Double problemSolving) {
        validateScore(programming, "programming");
        validateScore(framework, "framework");
        validateScore(problemSolving, "problemSolving");
        this.programming = programming;
        this.framework = framework;
        this.problemSolving = problemSolving;
    }

    public void validate() {
        validateScore(programming, "programming");
        validateScore(framework, "framework");
        validateScore(problemSolving, "problemSolving");
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
