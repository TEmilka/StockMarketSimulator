package org.example.stockmarketsimulator.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.example.stockmarketsimulator.exception.GlobalExceptionHandler;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.AssetsRepository;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

@ExtendWith(MockitoExtension.class)
public class UserControllerTests {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AssetsRepository assetsRepository;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    public void setup() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void getUsers_shouldReturnUsers() throws Exception {
        // Given
        User user = new User("John Doe", "john.doe@example.com", "password123");
        when(userRepository.findAll()).thenReturn(Collections.singletonList(user));

        // When & Then
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
    }

    @Test
    void addUser_shouldReturnCreatedUser() throws Exception {
        // Given
        User user = new User("John Doe", "john.doe@example.com", "password123");
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"John Doe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

    }

    @Test
    void addUser_shouldReturnBadRequest_whenNameOrEmailOrPasswordIsMissing() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"name\":\"John Doe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nazwa użytkownika, email i hasło są wymagane."))
                .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nazwa użytkownika, email i hasło są wymagane."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deleteUser_shouldReturnNoContent_whenUserExists() throws Exception {
        // Given
        Long userId = 1L;
        User user = new User("John Doe", "john.doe@example.com", "password123");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        // When & Then
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(userRepository, times(1)).deleteById(userId);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNotFound());

        verify(userRepository, never()).deleteById(userId);
    }

    @Test
    void getUserWalletDetails_shouldReturnWalletDetails() throws Exception {
        // Given
        Long userId = 1L;
        User user = new User("John Doe", "john.doe@example.com", "password123");
        UserWallet wallet = new UserWallet(user);
        wallet.addAsset(1L, 10.0);
        user.setWallet(wallet);

        Asset asset = new Asset("AAPL", 150.0, "Apple Inc.");
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assetsRepository.findById(1L)).thenReturn(Optional.of(asset));

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/wallet/details", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].price").value(150.0))
                .andExpect(jsonPath("$[0].amount").value(10.0));
    }

    @Test
    void getUserWalletDetails_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(get("/api/users/{userId}/wallet/details", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Użytkownik nie został znaleziony"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addAssetToWallet_shouldReturnUpdatedWallet() throws Exception {
        // Given
        Long userId = 1L;
        Long assetId = 1L;
        Double amount = 10.0;

        User user = new User("John Doe", "john.doe@example.com", "password123");
        Asset asset = new Asset("AAPL", 150.0, "Apple Inc.");
        UserWallet wallet = new UserWallet(user);
        user.setWallet(wallet);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(assetsRepository.findById(assetId)).thenReturn(Optional.of(asset));

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/wallet/add", userId)
                        .contentType("application/json")
                        .content("{\"assetId\":1,\"amount\":10.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].name").value("Apple Inc."))
                .andExpect(jsonPath("$[0].price").value(150.0))
                .andExpect(jsonPath("$[0].amount").value(10.0));
    }

    @Test
    void addAssetToWallet_shouldReturnNotFound_whenAssetOrUserDoesNotExist() throws Exception {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When & Then
        mockMvc.perform(post("/api/users/{userId}/wallet/add", userId)
                        .contentType("application/json")
                        .content("{\"assetId\":1,\"amount\":10.0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Użytkownik o ID 1 nie został znaleziony"))
                .andExpect(jsonPath("$.status").value(404));
    }
}
