package org.example.stockmarketsimulator.controller;

import org.example.stockmarketsimulator.dto.UserRegistrationDTO;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.example.stockmarketsimulator.exception.BadRequestException;

import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class AuthControllerTests {

    private MockMvc mockMvc;

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() throws Exception {
        // Given
        when(authService.authenticate("JohnDoe", "password123"))
                .thenReturn(Map.of(
                    "token", "mocked-jwt-token",
                    "userId", "1",
                    "role", "USER"
                ));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"JohnDoe\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("jwt"))
                .andExpect(cookie().value("jwt", "mocked-jwt-token"))
                .andExpect(jsonPath("$.userId").value("1"));
    }

    @Test
    void registerUser_shouldReturnCreated_whenUserIsValid() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"JohnDoe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(content().string("Użytkownik został pomyślnie zarejestrowany."));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenEmailAlreadyExists() throws Exception {
        // Given
        doThrow(new BadRequestException("Użytkownik o tym emailu już istnieje."))
                .when(authService).register(any(UserRegistrationDTO.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"JohnDoe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Użytkownik o tym emailu już istnieje."));
    }

    @Test
    void registerUser_shouldReturnBadRequest_whenUsernameAlreadyExists() throws Exception {
        // Given
        doThrow(new BadRequestException("Użytkownik o tej nazwie użytkownika już istnieje."))
                .when(authService).register(any(UserRegistrationDTO.class));

        // When & Then
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"JohnDoe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Użytkownik o tej nazwie użytkownika już istnieje."));
    }
}
