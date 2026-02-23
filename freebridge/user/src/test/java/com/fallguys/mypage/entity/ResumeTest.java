package com.fallguys.mypage.entity.resume;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ResumeTest {

    @Test
    @DisplayName("Resume 생성 및 업데이트 테스트")
    void createAndUpdateResume() {
        // given
        UUID userId = UUID.randomUUID();
        Resume resume = new Resume(userId);

        // when
        String name = "New Name";
        String birthDate = "1990-01-01";
        String phone = "010-1234-5678";
        String email = "test@test.com";
        String address = "Seoul";

        List<Education> educations = new ArrayList<>();
        educations.add(new Education("Univ", "Test Univ", "CS", "Graduated", "2010.03", "2014.02"));

        List<Career> careers = new ArrayList<>();
        careers.add(new Career("Company A", "Dev", "Junior", "Backend", "Full-time", "2014.03", "2016.02", "Worked hard"));

        List<Certification> certifications = new ArrayList<>();
        certifications.add(new Certification("Cert A", "Issuer A", "2015.05"));

        resume.update(name, birthDate, phone, email, address, educations, careers, certifications);

        // then
        assertThat(resume.getUserId()).isEqualTo(userId);
        assertThat(resume.getName()).isEqualTo(name);
        assertThat(resume.getBirthDate()).isEqualTo(birthDate);
        assertThat(resume.getEducations()).hasSize(1);
        assertThat(resume.getCareers()).hasSize(1);
        assertThat(resume.getCertifications()).hasSize(1);
        
        assertThat(resume.getEducations().get(0).getSchoolName()).isEqualTo("Test Univ");
        assertThat(resume.getCareers().get(0).getCompanyName()).isEqualTo("Company A");
        assertThat(resume.getCertifications().get(0).getName()).isEqualTo("Cert A");
    }
}
