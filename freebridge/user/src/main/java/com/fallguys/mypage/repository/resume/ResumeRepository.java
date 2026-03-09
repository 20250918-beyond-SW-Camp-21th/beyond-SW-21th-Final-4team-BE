package com.fallguys.mypage.repository.resume;

import com.fallguys.mypage.entity.resume.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {

    Optional<Resume> findByFreelancerId(Long freelancerId);

    @Query( "select r " +
            "  from Resume r " +
            " where r.freelancerId = (" +
            "select f.freelancerId " +
            "  from Freelancer f " +
            " where f.userId = :userId)")
    Optional<Resume> findByUserId(@Param("userId") Long userId);
}
