package com.fallguys.mypage.entity.freelancer;


import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Expertise {
    private Integer programming;
    private Integer framework;
    private Integer problemSolving;
}