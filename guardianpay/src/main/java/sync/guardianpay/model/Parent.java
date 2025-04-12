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
import java.util.List;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@Table(name = "parents")
public class Parent extends AbstractAuditingEntity{
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

    @ManyToMany(mappedBy = "parents", fetch = FetchType.LAZY)
    private List<Student> students = new ArrayList<>();
}
