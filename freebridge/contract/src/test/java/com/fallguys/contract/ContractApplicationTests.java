package com.fallguys.contract;

import com.fallguys.common.exception.BusinessException;
import com.fallguys.common.exception.ErrorCode;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.entity.ContractStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContractApplicationTests {

    private Contract contract;

    @BeforeEach
    void setUp() {
        contract = new Contract();
    }

    // ── signBy ───────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("signBy()")
    class SignBy {

        @Test
        @DisplayName("프리랜서가 서명하면 freelancerSignature와 freelancerSignedDate가 설정된다")
        void freelancer_setsSignatureAndDate() {
            contract.signBy("FREELANCER", "data:image/png;base64,abc");

            assertEquals("data:image/png;base64,abc", contract.getFreelancerSignature());
            assertNotNull(contract.getFreelancerSignedDate());
        }

        @Test
        @DisplayName("프리랜서 서명 시 고용주 서명은 변경되지 않는다")
        void freelancer_doesNotAffectEmployerSignature() {
            contract.signBy("FREELANCER", "data:image/png;base64,abc");

            assertNull(contract.getEmployerSignature());
            assertNull(contract.getEmployerSignedDate());
        }

        @Test
        @DisplayName("고용주가 서명하면 employerSignature와 employerSignedDate가 설정된다")
        void employer_setsSignatureAndDate() {
            contract.signBy("EMPLOYER", "data:image/png;base64,xyz");

            assertEquals("data:image/png;base64,xyz", contract.getEmployerSignature());
            assertNotNull(contract.getEmployerSignedDate());
        }

        @Test
        @DisplayName("고용주 서명 시 프리랜서 서명은 변경되지 않는다")
        void employer_doesNotAffectFreelancerSignature() {
            contract.signBy("EMPLOYER", "data:image/png;base64,xyz");

            assertNull(contract.getFreelancerSignature());
            assertNull(contract.getFreelancerSignedDate());
        }

        @Test
        @DisplayName("유효하지 않은 role로 서명 시 CONTRACT_FORBIDDEN 에러가 발생한다")
        void invalidRole_throws() {
            BusinessException ex = assertThrows(BusinessException.class,
                    () -> contract.signBy("ADMIN", "data:image/png;base64,abc"));

            assertEquals(ErrorCode.CONTRACT_FORBIDDEN, ex.getErrorCode());
        }
    }

    // ── isBothSigned ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isBothSigned()")
    class IsBothSigned {

        @Test
        @DisplayName("고용주만 서명하면 false를 반환한다")
        void onlyEmployerSigned_returnsFalse() {
            contract.setEmployerSignature("data:image/png;base64,emp");

            assertFalse(contract.isBothSigned());
        }

        @Test
        @DisplayName("프리랜서만 서명하면 false를 반환한다")
        void onlyFreelancerSigned_returnsFalse() {
            contract.setFreelancerSignature("data:image/png;base64,free");

            assertFalse(contract.isBothSigned());
        }

        @Test
        @DisplayName("아무도 서명하지 않으면 false를 반환한다")
        void noOneSigned_returnsFalse() {
            assertFalse(contract.isBothSigned());
        }

        @Test
        @DisplayName("양측 모두 서명하면 true를 반환한다")
        void bothSigned_returnsTrue() {
            contract.setEmployerSignature("data:image/png;base64,emp");
            contract.setFreelancerSignature("data:image/png;base64,free");

            assertTrue(contract.isBothSigned());
        }
    }

    // ── isActivatable ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isActivatable()")
    class IsActivatable {

        @Test
        @DisplayName("양측 서명 완료 + WAITING_SIGNATURE 상태이면 true를 반환한다")
        void bothSignedAndWaiting_returnsTrue() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);
            contract.setEmployerSignature("data:image/png;base64,emp");
            contract.setFreelancerSignature("data:image/png;base64,free");

            assertTrue(contract.isActivatable());
        }

        @Test
        @DisplayName("양측 서명 완료이지만 이미 IN_PROGRESS이면 false를 반환한다")
        void bothSignedButAlreadyInProgress_returnsFalse() {
            contract.setStatus(ContractStatus.IN_PROGRESS);
            contract.setEmployerSignature("data:image/png;base64,emp");
            contract.setFreelancerSignature("data:image/png;base64,free");

            assertFalse(contract.isActivatable());
        }

        @Test
        @DisplayName("WAITING_SIGNATURE이지만 한쪽만 서명하면 false를 반환한다")
        void waitingButOnlyOneSigned_returnsFalse() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);
            contract.setEmployerSignature("data:image/png;base64,emp");

            assertFalse(contract.isActivatable());
        }
    }

    @Nested
    @DisplayName("activate()")
    class Activate {

        @Test
        @DisplayName("양측 서명 완료 + WAITING_SIGNATURE 상태에서 activate() 호출 시 IN_PROGRESS로 변경된다")
        void setsInProgressStatus() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);
            contract.setEmployerSignature("data:image/png;base64,emp");
            contract.setFreelancerSignature("data:image/png;base64,free");

            contract.activate();

            assertEquals(ContractStatus.IN_PROGRESS, contract.getStatus());
        }

        @Test
        @DisplayName("양측 서명 완료 + WAITING_SIGNATURE 상태에서 activate() 호출 시 signedDate가 설정된다")
        void setsSignedDate() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);
            contract.setEmployerSignature("data:image/png;base64,emp");
            contract.setFreelancerSignature("data:image/png;base64,free");

            contract.activate();

            assertNotNull(contract.getSignedDate());
        }

        @Test
        @DisplayName("서명이 없는 상태에서 activate() 호출 시 CONTRACT_NOT_ACTIVATABLE 에러가 발생한다")
        void whenNotActivatable_throws() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);
            // 서명 없음

            BusinessException ex = assertThrows(BusinessException.class, contract::activate);
            assertEquals(ErrorCode.CONTRACT_NOT_ACTIVATABLE, ex.getErrorCode());
        }

        @Test
        @DisplayName("이미 IN_PROGRESS 상태에서 activate() 호출 시 CONTRACT_NOT_ACTIVATABLE 에러가 발생한다")
        void whenAlreadyInProgress_throws() {
            contract.setStatus(ContractStatus.IN_PROGRESS);
            contract.setEmployerSignature("data:image/png;base64,emp");
            contract.setFreelancerSignature("data:image/png;base64,free");

            BusinessException ex = assertThrows(BusinessException.class, contract::activate);
            assertEquals(ErrorCode.CONTRACT_NOT_ACTIVATABLE, ex.getErrorCode());
        }
    }


    @Nested
    @DisplayName("complete()")
    class Complete {

        @Test
        @DisplayName("IN_PROGRESS 상태에서 complete() 호출 시 COMPLETED로 변경된다")
        void whenInProgress_setsCompleted() {
            contract.setStatus(ContractStatus.IN_PROGRESS);

            contract.complete();

            assertEquals(ContractStatus.COMPLETED, contract.getStatus());
        }

        @Test
        @DisplayName("WAITING_SIGNATURE 상태에서 complete() 호출 시 CONTRACT_NOT_IN_PROGRESS 에러가 발생한다")
        void whenWaitingSignature_throws() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);

            BusinessException ex = assertThrows(BusinessException.class, contract::complete);
            assertEquals(ErrorCode.CONTRACT_NOT_IN_PROGRESS, ex.getErrorCode());
        }

        @Test
        @DisplayName("REJECTED 상태에서 complete() 호출 시 CONTRACT_NOT_IN_PROGRESS 에러가 발생한다")
        void whenRejected_throws() {
            contract.setStatus(ContractStatus.REJECTED);

            BusinessException ex = assertThrows(BusinessException.class, contract::complete);
            assertEquals(ErrorCode.CONTRACT_NOT_IN_PROGRESS, ex.getErrorCode());
        }

        @Test
        @DisplayName("이미 COMPLETED 상태에서 complete() 호출 시 CONTRACT_NOT_IN_PROGRESS 에러가 발생한다")
        void whenAlreadyCompleted_throws() {
            contract.setStatus(ContractStatus.COMPLETED);

            BusinessException ex = assertThrows(BusinessException.class, contract::complete);
            assertEquals(ErrorCode.CONTRACT_NOT_IN_PROGRESS, ex.getErrorCode());
        }
    }


    @Nested
    @DisplayName("reject()")
    class Reject {

        @Test
        @DisplayName("WAITING_SIGNATURE 상태에서 reject() 호출 시 REJECTED로 변경된다")
        void whenWaitingSignature_setsRejected() {
            contract.setStatus(ContractStatus.WAITING_SIGNATURE);

            contract.reject();

            assertEquals(ContractStatus.REJECTED, contract.getStatus());
        }

        @Test
        @DisplayName("IN_PROGRESS 상태에서 reject() 호출 시 CONTRACT_CANNOT_REJECT 에러가 발생한다")
        void whenInProgress_throws() {
            contract.setStatus(ContractStatus.IN_PROGRESS);

            BusinessException ex = assertThrows(BusinessException.class, contract::reject);
            assertEquals(ErrorCode.CONTRACT_CANNOT_REJECT, ex.getErrorCode());
        }

        @Test
        @DisplayName("COMPLETED 상태에서 reject() 호출 시 CONTRACT_CANNOT_REJECT 에러가 발생한다")
        void whenCompleted_throws() {
            contract.setStatus(ContractStatus.COMPLETED);

            BusinessException ex = assertThrows(BusinessException.class, contract::reject);
            assertEquals(ErrorCode.CONTRACT_CANNOT_REJECT, ex.getErrorCode());
        }
    }
}