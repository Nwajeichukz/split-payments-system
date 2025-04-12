package sync.guardianpay.dto.request;

import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProcessingDto {
    @NotEmpty(message = "parentId should not be blank")
    private String parentId;

    @NotEmpty(message = "studentId should not be blank")
    private String studentId;

    @NotNull(message = "amount must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
    private BigDecimal paymentAmount;

}