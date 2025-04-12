package sync.guardianpay.service;

import lombok.RequiredArgsConstructor;
import sync.guardianpay.dto.request.AdminRegistrationDto;
import sync.guardianpay.dto.request.AuthenticationDto;
import sync.guardianpay.dto.request.RegistrationDto;
import sync.guardianpay.dto.response.AppResponse;

import javax.validation.Valid;


public interface UserAuthService {
    AppResponse<String> adminSignUp(AdminRegistrationDto registrationDto);

    AppResponse<String> usersSignUp(@Valid RegistrationDto registrationDto);

    AppResponse<String> signIn(@Valid AuthenticationDto authenticationDto);
}
