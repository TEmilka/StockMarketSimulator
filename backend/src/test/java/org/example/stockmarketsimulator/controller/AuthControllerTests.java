package org.example.stockmarketsimulator.controller;

import java.util.Optional;

import org.example.stockmarketsimulator.dto.UserRegistrationDTO;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.example.stockmarketsimulator.security.JwtUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTests {

    private MockMvc mockMvc;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder; // Mock PasswordEncoder

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // Given
        User user = new User("JohnDoe", "john.doe@example.com", "password123");
        user.setId(1L); // Set the ID for the mocked user
        when(userRepository.findByUsername("JohnDoe")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mock(Authentication.class));
        when(jwtUtils.generateToken("JohnDoe")).thenReturn("mocked-jwt-token");

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"JohnDoe\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mocked-jwt-token"))
                .andExpect(jsonPath("$.userId").value("1")); // Ensure the ID matches the mocked user
    }

    @Test
    void registerUser_shouldReturnCreated_whenUserIsValid() throws Exception {
        // Given
        UserRegistrationDTO userDTO = new UserRegistrationDTO("JohnDoe", "john.doe@example.com", "password123");
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(false);
        when(userRepository.existsByUsername("JohnDoe")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword123"); // Mock encoding

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .characterEncoding("UTF-8")
                        .content("{\"username\":\"JohnDoe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Użytkownik został pomyślnie zarejestrowany."));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        // Given
        when(userRepository.existsByEmail("john.doe@example.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"JohnDoe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Użytkownik o tym emailu już istnieje."));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenUsernameAlreadyExists() throws Exception {
        // Given
        when(userRepository.existsByUsername("JohnDoe")).thenReturn(true);

        // When & Then
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"JohnDoe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Użytkownik o tej nazwie użytkownika już istnieje."));
    }
}
