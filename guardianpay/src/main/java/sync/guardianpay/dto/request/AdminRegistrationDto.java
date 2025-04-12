package sync.guardianpay.dto.request;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;

@Data
public class AdminRegistrationDto {
    @NotEmpty(message = "firstName should not be blank")
    private String firstName;

    @NotEmpty(message = "lastName should not be blank")
    private String lastName;

    @NotEmpty(message = "email should not be blank")
    @Email(message = "use email format")
    private String email;

    @NotEmpty(message = "password should not be blank")
    private String password;

    @NotEmpty(message = "confirmPassword should not be blank")
    private String confirmPassword;

}
