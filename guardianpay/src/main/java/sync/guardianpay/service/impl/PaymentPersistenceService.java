package sync.guardianpay.service.impl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import sync.guardianpay.model.Payment;
import sync.guardianpay.repository.TransactionRepository;

@Service
@RequiredArgsConstructor
public class PaymentPersistenceService {
    private final TransactionRepository transactionRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveFailedTransaction(Payment payment) {
        transactionRepository.save(payment);
    }

}
