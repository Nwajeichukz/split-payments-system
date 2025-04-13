package sync.guardianpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sync.guardianpay.model.Admin;


public interface AdminRepository extends JpaRepository<Admin, String> {
}
