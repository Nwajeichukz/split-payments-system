package sync.guardianpay.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sync.guardianpay.dto.request.AdminRegistrationDto;
import sync.guardianpay.dto.request.AuthenticationDto;
import sync.guardianpay.dto.request.RegistrationDto;
import sync.guardianpay.dto.response.AppResponse;
import sync.guardianpay.service.UserAuthService;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth/")
public class UserAuthController {
    private final UserAuthService userAuthService;

    @PostMapping("admin-signup")
    public AppResponse<String> adminSignUp(@Valid @RequestBody AdminRegistrationDto registrationDto){
        return userAuthService.adminSignUp(registrationDto);
    }

    @PostMapping("user-signup")
    public AppResponse<String> usersSignUp(@Valid @RequestBody RegistrationDto registrationDto){
        return userAuthService.usersSignUp(registrationDto);
    }

    @PostMapping("signin")
    public AppResponse<String> signIn(@Valid @RequestBody AuthenticationDto authenticationDto){
        return userAuthService.signIn(authenticationDto);
    }

}
