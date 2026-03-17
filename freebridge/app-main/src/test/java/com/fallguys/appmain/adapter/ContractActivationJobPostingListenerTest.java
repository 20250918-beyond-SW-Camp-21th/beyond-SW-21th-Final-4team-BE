package com.fallguys.appmain.adapter;

import com.fallguys.contract.api.shared.ContractActivatedEvent;
import com.fallguys.contract.entity.Contract;
import com.fallguys.contract.repository.ContractRepository;
import com.fallguys.recruitment.service.JobPostingService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContractActivationJobPostingListenerTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private JobPostingService jobPostingService;

    @InjectMocks
    private ContractActivationJobPostingListener listener;

    @Test
    @DisplayName("계약 활성화 이벤트 수신 시 relatedJobId에 해당하는 공고를 마감 처리한다")
    void handle_closesRelatedJobPosting() {
        Contract contract = new Contract();
        contract.setContractId(1001L);
        contract.setRelatedJobId("123");
        ReflectionTestUtils.setField(contract, "id", 1L, Long.class);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        listener.handle(new ContractActivatedEvent(this, 1L));

        verify(jobPostingService).closeJobPosting(123L);
    }

    @Test
    @DisplayName("relatedJobId가 없으면 공고 마감 처리를 건너뛴다")
    void handle_skipsWhenRelatedJobIdMissing() {
        Contract contract = new Contract();
        contract.setContractId(1001L);
        ReflectionTestUtils.setField(contract, "id", 1L, Long.class);

        when(contractRepository.findById(1L)).thenReturn(Optional.of(contract));

        listener.handle(new ContractActivatedEvent(this, 1L));

        verify(jobPostingService, never()).closeJobPosting(org.mockito.ArgumentMatchers.anyLong());
    }
}
