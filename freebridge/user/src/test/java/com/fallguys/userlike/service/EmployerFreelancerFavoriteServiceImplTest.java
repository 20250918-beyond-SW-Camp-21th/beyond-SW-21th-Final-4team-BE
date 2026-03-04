package com.fallguys.userlike.service;

import com.fallguys.userlike.repository.EmployerFreelancerFavoriteRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployerFreelancerFavoriteServiceImplTest {

    @Mock
    private EmployerFreelancerFavoriteRepository favoriteRepository;

    @InjectMocks
    private EmployerFreelancerFavoriteServiceImpl service;

    @Test
    @DisplayName("[TDD] 즐겨찾기 등록 시 Repository.save를 호출한다")
    void addFavorite_callsSave() {
        // given
        Long employerId = 10L;
        Long freelancerId = 20L;

        // when
        service.addFavorite(employerId, freelancerId);

        // then
        ArgumentCaptor<com.fallguys.userlike.entity.EmployerFreelancerFavorite> captor =
                ArgumentCaptor.forClass(com.fallguys.userlike.entity.EmployerFreelancerFavorite.class);
        verify(favoriteRepository, times(1)).save(captor.capture());
        assertEquals(employerId, captor.getValue().getEmployerId());
        assertEquals(freelancerId, captor.getValue().getFreelancerId());
    }

    @Test
    @DisplayName("[TDD] 즐겨찾기 등록 시 중복 키(SQLState 23505)는 예외 없이 무시한다")
    void addFavorite_duplicateBySqlState_isNoOp() {
        // given
        SQLException sqlException = new SQLException("duplicate key", "23505");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate", sqlException);
        when(favoriteRepository.save(any())).thenThrow(exception);

        // when & then
        assertDoesNotThrow(() -> service.addFavorite(1L, 2L));
        verify(favoriteRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("[TDD] 즐겨찾기 등록 시 중복 키(MySQL 1062)는 예외 없이 무시한다")
    void addFavorite_duplicateByMysqlErrorCode_isNoOp() {
        // given
        SQLException sqlException = new SQLException("Duplicate entry", "23000", 1062);
        DataIntegrityViolationException exception = new DataIntegrityViolationException("duplicate", sqlException);
        when(favoriteRepository.save(any())).thenThrow(exception);

        // when & then
        assertDoesNotThrow(() -> service.addFavorite(1L, 2L));
    }

    @Test
    @DisplayName("[TDD] 즐겨찾기 등록 시 비중복 무결성 예외는 그대로 전파한다")
    void addFavorite_nonDuplicateViolation_throwsException() {
        // given
        SQLException sqlException = new SQLException("constraint failure", "22001");
        DataIntegrityViolationException exception = new DataIntegrityViolationException("constraint violation", sqlException);
        when(favoriteRepository.save(any())).thenThrow(exception);

        // when & then
        DataIntegrityViolationException thrown =
                assertThrows(DataIntegrityViolationException.class, () -> service.addFavorite(1L, 2L));
        assertEquals("constraint violation", thrown.getMessage());
    }

    @Test
    @DisplayName("[TDD] 즐겨찾기 삭제 시 employerId/freelancerId로 삭제 메서드를 호출한다")
    void removeFavorite_callsDeleteByEmployerIdAndFreelancerId() {
        // given
        Long employerId = 7L;
        Long freelancerId = 9L;

        // when
        service.removeFavorite(employerId, freelancerId);

        // then
        verify(favoriteRepository, times(1))
                .deleteByEmployerIdAndFreelancerId(employerId, freelancerId);
    }
}
