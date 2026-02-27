package com.fallguys.userlike.service;

import com.fallguys.userlike.entity.EmployerFreelancerFavorite;
import com.fallguys.userlike.repository.EmployerFreelancerFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmployerFreelancerFavoriteServiceImpl implements EmployerFreelancerFavoriteService {

    private final EmployerFreelancerFavoriteRepository favoriteRepository;

    @Override
    @Transactional
    public void addFavorite(Long employerId, Long freelancerId) {
        try {
            favoriteRepository.save(EmployerFreelancerFavorite.of(employerId, freelancerId));
        } catch (DataIntegrityViolationException ignored) {
            // Duplicate favorite is treated as idempotent no-op.
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long employerId, Long freelancerId) {
        favoriteRepository.deleteByEmployerIdAndFreelancerId(employerId, freelancerId);
    }
}
