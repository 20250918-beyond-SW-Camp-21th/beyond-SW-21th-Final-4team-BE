package com.fallguys.payment.service;

import com.fallguys.payment.entity.BillingKey;
import com.fallguys.payment.repository.BillingKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class            BillingKeyService {

    private final BillingKeyRepository billingKeyRepository;

    @Transactional(readOnly = true)
    public Optional<BillingKey> getActiveBillingKey(Long employerId) {
        return billingKeyRepository.findByEmployerIdAndActiveTrue(employerId);
    }

    @Transactional(readOnly = true)
    public List<BillingKey> getAllActiveBillingKeys() {
        return billingKeyRepository.findByActiveTrue();
    }

    @Transactional
    public void deactivateBillingKey(Long employerId) {
        billingKeyRepository.findByEmployerIdAndActiveTrue(employerId)
                .ifPresent(bk -> {
                    bk.deactivate();
                    billingKeyRepository.save(bk);
                });
    }
}