package com.fallguys.mypage.entity;

import com.fallguys.mypage.entity.freelancer.Freelancer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class FreelancerTest {

    @Test
    @DisplayName("Freelancer 생성 성공 테스트")
    void createFreelancer_success() {
        // given
        UUID id = UUID.randomUUID();
        String name = "Test Freelancer";
        String email = "test@example.com";
        String introduction = "Hello, I am a freelancer.";

        // when
        Freelancer freelancer = new Freelancer(id, name, email, introduction);

        // then
        assertThat(freelancer.getId()).isEqualTo(id);
        assertThat(freelancer.getName()).isEqualTo(name);
        assertThat(freelancer.getEmail()).isEqualTo(email);
        assertThat(freelancer.getIntroduction()).isEqualTo(introduction);
    }
}
