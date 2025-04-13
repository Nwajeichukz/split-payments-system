package sync.guardianpay.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import sync.guardianpay.dto.request.AdminRegistrationDto;
import sync.guardianpay.dto.request.AuthenticationDto;
import sync.guardianpay.model.Admin;
import sync.guardianpay.model.Role;
import sync.guardianpay.model.User;
import sync.guardianpay.repository.AdminRepository;
import sync.guardianpay.repository.RoleRepository;
import sync.guardianpay.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class UserAuthControllerTest {
    @LocalServerPort
    public int port;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    public PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdminRepository adminRepository;

    @Autowired
    private RoleRepository roleRepository;


    public static final String ADMIN_EMAIL = "admintest@mocx.co";
    public static final String TEST_PASSWORD = "Fakepassword@123";

    @AfterEach
    public void tearDown() {
        adminRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }


    @Test
    void adminSignUp_withValidData_shouldReturnSuccess() throws Exception {
        AdminRegistrationDto adminRegistrationDto = new AdminRegistrationDto();
        adminRegistrationDto.setEmail(ADMIN_EMAIL);
        adminRegistrationDto.setFirstName("sam");
        adminRegistrationDto.setLastName("john");
        adminRegistrationDto.setConfirmPassword(TEST_PASSWORD);
        adminRegistrationDto.setPassword(TEST_PASSWORD);


        mockMvc.perform(post("/api/v1/auth/admin-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegistrationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.message").value(adminRegistrationDto.getLastName() + " your account has been created"));

        // Verify database state
        User savedUser = userRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        assertThat(savedUser.getFirstName()).isEqualTo("sam");
        assertThat(userRepository.findByEmail(ADMIN_EMAIL)).isPresent();
    }

    @Test
    void adminSignUp_withPasswordMismatch_shouldReturnSuccessWithErrorStatus() throws Exception {
        AdminRegistrationDto adminRegistrationDto = new AdminRegistrationDto();
        adminRegistrationDto.setEmail("mismatch@test.com");
        adminRegistrationDto.setFirstName("test");
        adminRegistrationDto.setLastName("user");
        adminRegistrationDto.setPassword(TEST_PASSWORD);
        adminRegistrationDto.setConfirmPassword("DifferentPassword123");

        mockMvc.perform(post("/api/v1/auth/admin-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegistrationDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(-1))
                .andExpect(jsonPath("$.message").value("Passwords do not match"));

        assertThat(userRepository.findByEmail("mismatch@test.com")).isEmpty();
    }

    @Test
    void adminSignUp_withEmptyFields_shouldReturnBadRequest() throws Exception {
        AdminRegistrationDto adminRegistrationDto = new AdminRegistrationDto();
        adminRegistrationDto.setEmail("mismatch@test.com");
        adminRegistrationDto.setLastName("user");
        adminRegistrationDto.setPassword(TEST_PASSWORD);
        adminRegistrationDto.setConfirmPassword("DifferentPassword123");


        mockMvc.perform(post("/api/v1/auth/admin-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminRegistrationDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(-1))
                .andExpect(jsonPath("$.message").value(containsString("firstName should not be blank")));
    }

    @Test
    void adminSignUp_withExistingEmail_shouldReturnBadRequest() throws Exception {
        // Try to create same admin again
        AdminRegistrationDto duplicateDto = new AdminRegistrationDto();
        duplicateDto.setEmail(ADMIN_EMAIL);
        duplicateDto.setFirstName("sam");
        duplicateDto.setLastName("john");
        duplicateDto.setPassword(TEST_PASSWORD);
        duplicateDto.setConfirmPassword(TEST_PASSWORD);

        createAdmin(duplicateDto);

        mockMvc.perform(post("/api/v1/auth/admin-signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(0)) // Your error status code
                .andExpect(jsonPath("$.message").value(containsString("user already exist login")));

        // Verify only one user exists with this email
        assertThat(userRepository.findByEmail(ADMIN_EMAIL)).isPresent();
    }


    @Test
    void signIn_withValidCredentials_shouldReturnSuccess() throws Exception {
        // First create a test admin user
        AdminRegistrationDto registrationDto = new AdminRegistrationDto();
        registrationDto.setEmail(ADMIN_EMAIL);
        registrationDto.setFirstName("Test");
        registrationDto.setLastName("User");
        registrationDto.setPassword(TEST_PASSWORD);
        registrationDto.setConfirmPassword(TEST_PASSWORD);
        createAdmin(registrationDto);

        // Prepare sign-in request
        AuthenticationDto authDto = new AuthenticationDto();
        authDto.setEmail(ADMIN_EMAIL);
        authDto.setPassword(TEST_PASSWORD);

        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(0))
                .andExpect(jsonPath("$.message").value("successful signin"))
                .andExpect(jsonPath("$.data").exists()); // Verify token exists
    }

    @Test
    void signIn_withInvalidPassword_shouldReturnError() throws Exception {
        // Create test user
        AdminRegistrationDto registrationDto = new AdminRegistrationDto();
        registrationDto.setEmail(ADMIN_EMAIL);
        registrationDto.setFirstName("Test");
        registrationDto.setLastName("User");
        registrationDto.setPassword(TEST_PASSWORD);
        registrationDto.setConfirmPassword(TEST_PASSWORD);
        createAdmin(registrationDto);

        // Attempt sign-in with wrong password
        AuthenticationDto authDto = new AuthenticationDto();
        authDto.setEmail(ADMIN_EMAIL);
        authDto.setPassword("wrongPassword");

        mockMvc.perform(post("/api/v1/auth/signin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(authDto)))
                .andExpect(status().isOk()) // Now expecting 401
                .andExpect(jsonPath("$.status").value(-1))
                .andExpect(jsonPath("$.message").value("wrong email or password"));
    }

    private Role createRole(String roleName) {
        Optional<Role> role = roleRepository.findByName(roleName);

        if (role.isEmpty()) {
            Role newRole = new Role();
            newRole.setName(roleName);

            return newRole;
        }

        return role.get();
    }

    private Admin createAdmin(AdminRegistrationDto adminRegistrationDto) {
        User user = User.builder()
                .email(ADMIN_EMAIL)
                .firstName(adminRegistrationDto.getFirstName())
                .lastName(adminRegistrationDto.getLastName())
                .password(passwordEncoder.encode(TEST_PASSWORD))
                .role(createRole("ADMIN"))
                .build();

        Admin admin = Admin.builder()
                .user(user)
                .build();
        return adminRepository.save(admin);
    }

}
