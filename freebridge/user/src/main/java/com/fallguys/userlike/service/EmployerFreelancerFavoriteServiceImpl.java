package com.fallguys.userlike.service;

import com.fallguys.userlike.entity.EmployerFreelancerFavorite;
import com.fallguys.userlike.repository.EmployerFreelancerFavoriteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.SQLException;
import java.util.Locale;

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
        } catch (DataIntegrityViolationException ex) {
            if (!isDuplicateKeyViolation(ex)) {
                throw ex;
            }
            // Duplicate favorite is treated as idempotent no-op.
        }
    }

    @Override
    @Transactional
    public void removeFavorite(Long employerId, Long freelancerId) {
        favoriteRepository.deleteByEmployerIdAndFreelancerId(employerId, freelancerId);
    }
    //중복 입력 조절
    private boolean isDuplicateKeyViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                String sqlState = sqlException.getSQLState();
                if ("23505".equals(sqlState)) {
                    return true;
                }
                if (sqlException.getErrorCode() == 1062) {
                    return true;
                }
            }

            String message = current.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains("duplicate")) {
                return true;
            }

            current = current.getCause();
        }
        return false;
    }
}
