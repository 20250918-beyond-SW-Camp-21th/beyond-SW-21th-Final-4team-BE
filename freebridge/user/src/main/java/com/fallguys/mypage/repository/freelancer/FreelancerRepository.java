package com.fallguys.mypage.repository.freelancer;

import com.fallguys.mypage.entity.freelancer.Freelancer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerRepository extends JpaRepository<Freelancer, Long> {

    Optional<Freelancer> findByUserId(Long userId);

    List<Freelancer> findAllByUserIdIn(Collection<Long> userIds);

    @Query(
            value = """
                select distinct f
                from Freelancer f
                join User u on u.id = f.userId
                left join f.skills s
                where (:keyword is null or :keyword = ''
                    or lower(u.name) like lower(concat('%', :keyword, '%'))
                    or lower(s) like lower(concat('%', :keyword, '%')))
            """,
            countQuery = """
                select count(distinct f)
                from Freelancer f
                join User u on u.id = f.userId
                left join f.skills s
                where (:keyword is null or :keyword = ''
                    or lower(u.name) like lower(concat('%', :keyword, '%'))
                    or lower(s) like lower(concat('%', :keyword, '%')))
            """
    )
    Page<Freelancer> searchFreelancers(@Param("keyword") String keyword,
                                       Pageable pageable);

}
