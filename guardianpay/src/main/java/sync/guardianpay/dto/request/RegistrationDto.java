package sync.guardianpay.dto.request;

import lombok.Data;
import sync.guardianpay.enums.UserRolesEnums;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Set;

@Data
public class RegistrationDto {
    @NotEmpty(message = "firstName should not be blank")
    private String firstName;

    @NotEmpty(message = "lastName should not be blank")
    private String lastName;

    @NotEmpty(message = "email should not be blank")
    @Email(message = "use email format")
    private String email;

    @javax.validation.constraints.Pattern(
            regexp = "STUDENT|PARENT",
            message = "role should be either STUDENT, PARENT,"
    )    private String role;

    @NotEmpty(message = "password should not be blank")
    private String password;

    @NotEmpty(message = "confirmPassword should not be blank")
    private String confirmPassword;

    @NotNull(message = "amount must not be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "amount must be greater than 0")
    private BigDecimal amount;

    private Set<String> familyIds ;
}
