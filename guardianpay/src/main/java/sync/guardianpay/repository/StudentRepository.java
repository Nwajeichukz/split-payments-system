package sync.guardianpay.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import sync.guardianpay.model.Student;

public interface StudentRepository extends JpaRepository<Student, String> {
}
