package sync.guardianpay.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import sync.guardianpay.model.Role;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(String name);


}
