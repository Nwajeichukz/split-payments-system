package sync.guardianpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sync.guardianpay.model.Parent;

public interface ParentRepository extends JpaRepository<Parent, String> {
}
