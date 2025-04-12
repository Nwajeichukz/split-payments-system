package sync.guardianpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sync.guardianpay.model.Payment;

public interface TransactionRepository extends JpaRepository<Payment, String> {
}
