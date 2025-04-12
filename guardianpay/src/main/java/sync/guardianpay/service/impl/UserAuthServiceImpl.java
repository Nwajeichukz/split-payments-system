package sync.guardianpay.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import sync.guardianpay.dto.request.AdminRegistrationDto;
import sync.guardianpay.dto.request.AuthenticationDto;
import sync.guardianpay.dto.request.RegistrationDto;
import sync.guardianpay.dto.response.AppResponse;
import sync.guardianpay.enums.UserRolesEnums;
import sync.guardianpay.exception.ApiException;
import sync.guardianpay.model.*;
import sync.guardianpay.repository.*;
import sync.guardianpay.service.UserAuthService;
import sync.guardianpay.service.jwt.JwtService;
import sync.guardianpay.service.jwt.MyUserDetailsService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {
    private final UserRepository userRepository;

    private final ParentRepository parentRepository;

    private final StudentRepository studentRepository;

    private final PasswordEncoder passwordEncoder;

    private final AdminRepository adminRepository;

    private final JwtService jwtService;

    private final RoleRepository roleRepository;

    private final MyUserDetailsService myUserDetailsService;
    @Override
    public AppResponse<String> adminSignUp(AdminRegistrationDto registrationDto) {
        emailCheck(registrationDto.getEmail());

        if (!registrationDto.getPassword().equals(registrationDto.getConfirmPassword())) {
            return new AppResponse<>(-1, "Passwords do not match");
        }

        // build user entity
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(getUserRole(UserRolesEnums.ADMIN.toString()));

        // build and save admin entity
        Admin admin = Admin.builder()
                .user(user)
                .build();

        Admin savedUser = adminRepository.save(admin);


        return new AppResponse<>(0, savedUser.getUser().getLastName() + " your account has been created");
    }

    @Override
    public AppResponse<String> usersSignUp(RegistrationDto registrationDto) {

        emailCheck(registrationDto.getEmail());

        // dynamically register based on role
        switch (registrationDto.getRole()){
            case "STUDENT": return registerStudent(registrationDto);
            case "PARENT": return  registerParent(registrationDto);
            default: throw new ApiException("Unsupported user role");
        }
    }

    public AppResponse<String> signIn(AuthenticationDto authenticationDto) {

        // Load user details by email
        var user = myUserDetailsService.loadUserByUsername(authenticationDto.getEmail());

        // check password match
        if(!passwordEncoder.matches(authenticationDto.getPassword(), user.getPassword()))
            return new AppResponse<>(-1, "wrong email or password");

        String token = jwtService.generateToken(user);

        return new AppResponse<>(0, "successful signin", token);
    }


    private void emailCheck(String email){
        boolean userCheck = userRepository.existsByEmail(email);

        if(userCheck) throw new ApiException("user already exist login");
    }

    private AppResponse<String> registerStudent(RegistrationDto registrationDto){
        //process parent ids if provided
        List<Parent> checkedParents = processParentIds(registrationDto.getFamilyIds());

        //Create User Entity
        User user = createUserEntity(registrationDto);

        //Build and save Student
        Student student = Student.builder()
                .balance(registrationDto.getAmount())
                .user(user)
                .balance(validateInitialBalance(registrationDto.getAmount()))
                .parents(new ArrayList<>())
                .build();

        // Establish bidirectional relationships
        for (Parent parent : checkedParents) {
            student.addParent(parent);
        }


        Student savedStudent = studentRepository.save(student);

        return new AppResponse<>(0,savedStudent.getUser().getFirstName() + " account successfully created");
    }


    AppResponse<String> registerParent(RegistrationDto registrationDto){
        //Create User Entity
        User user = createUserEntity(registrationDto);

        //Build and save Parent
        Parent parent = Parent.builder()
                .user(user)
                .balance(validateInitialBalance(registrationDto.getAmount()))
                .build();

        Parent savedParent = parentRepository.save(parent);

        return new AppResponse<>(0,savedParent.getUser().getFirstName() + " account successfully created");

    }

    // generic method to create and return a User entity from RegistrationDto
    private User createUserEntity(RegistrationDto registrationDto){
        User user = new User();
        user.setEmail(registrationDto.getEmail());
        user.setFirstName(registrationDto.getFirstName());
        user.setLastName(registrationDto.getLastName());
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRole(getUserRole(String.valueOf(registrationDto.getRole())));

        return user;
    }


    // resolved list of parents from provided ids
    private List<Parent> processParentIds(Set<String> parentsIds){
        if (parentsIds.isEmpty()) throw new ApiException("Parents Ids can not be empty");


        return parentsIds.stream()
                .map(parentId -> parentRepository.findById(parentId)
                        .orElseThrow(() -> new ApiException("Parent With this id "+ parentId + " not found")))
                .collect(Collectors.toList());

    }

    // ensure initial balance is valid
    private BigDecimal validateInitialBalance(BigDecimal amount) {
        if (amount == null) {
            return BigDecimal.ZERO;
        }
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new ApiException("Initial balance cannot be negative");
        }
        return amount;
    }

    // fetch role from DB or create new one if not found
    private Role getUserRole(String roleName){
        Optional<Role> role = roleRepository.findByName(roleName);

        if (role.isEmpty()) {
            Role newRole = new Role();
            newRole.setName(roleName);

            return newRole;
        }

        return role.get();
    }
}