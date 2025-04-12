package sync.guardianpay.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@Table(name = "admins")
public class Admin extends AbstractAuditingEntity {
    @JsonIgnore
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    @OneToOne(cascade = CascadeType.ALL)
    private User user;
}
