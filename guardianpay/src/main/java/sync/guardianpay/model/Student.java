package sync.guardianpay.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@Table(name = "students")
public class Student extends AbstractAuditingEntity {
    @JsonIgnore
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne(cascade = CascadeType.ALL)
    private User user;

    @Column(nullable = false)
    private BigDecimal balance;

    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "parent_student",
            joinColumns = @JoinColumn(name = "student_id"),
            inverseJoinColumns = @JoinColumn(name = "parent_id")
    )
    private List<Parent> parents  = new ArrayList<>();

    // Helper method to maintain both sides of relationship
    public void addParent(Parent parent) {
        this.parents.add(parent);
        parent.getStudents().add(this);
    }
}
