package sync.guardianpay.model;


import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.GenericGenerator;
import sync.guardianpay.enums.PaymentStatusEnum;

import javax.persistence.*;
import java.math.BigDecimal;

@Entity
@SuperBuilder
@Getter
@Setter
@NoArgsConstructor
@Table(name = "payments")
public class Payment extends AbstractAuditingEntity {
    @JsonIgnore
    @Id
    @GeneratedValue(generator = "uuid")
    @GenericGenerator(name = "uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(length = 36, nullable = false, updatable = false)
    private String id;

    private String parentId;

    private String studentId;

    private BigDecimal originalAmount;

    private BigDecimal adjustedAmount;

    private BigDecimal dynamicRate;

    private PaymentStatusEnum status;

}
