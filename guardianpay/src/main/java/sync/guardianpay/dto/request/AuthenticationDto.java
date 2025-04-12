package sync.guardianpay.dto.request;

import lombok.Data;

@Data
public class AuthenticationDto {
    private String email;

    private String password;
}
