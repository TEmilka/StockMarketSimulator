package org.example.stockmarketsimulator.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.exception.GlobalExceptionHandler;
import org.example.stockmarketsimulator.exception.ResourceNotFoundException;
import org.example.stockmarketsimulator.model.Asset;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.*;

/**
 * Pokrycie testami endpointów UserController:
 * - GET /api/v1/users
 * - POST /api/v1/users
 * - DELETE /api/v1/users/{id}
 * - GET /api/v1/users/{userId}/wallet/details
 * - POST /api/v1/users/{userId}/wallet/add
 * - GET /api/v1/users/{id}
 * - POST /api/v1/users/{id}/add-funds
 * - POST /api/v1/users/{userId}/wallet/trade
 * - GET /api/v1/users/{userId}/transactions
 * - GET /api/v1/users/{userId}/aggregated
 */

@ExtendWith(MockitoExtension.class)
public class UserControllerTests {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

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
        when(userService.getAllUsers()).thenReturn(Collections.singletonList(user));

        // When & Then
        mockMvc.perform(get("/api/v1/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("John Doe"))
                .andExpect(jsonPath("$[0].email").value("john.doe@example.com"));
    }

    @Test
    void addUser_shouldReturnCreatedUser() throws Exception {
        // Given
        User user = new User("John Doe", "john.doe@example.com", "password123");
        when(userService.createUser(any(User.class))).thenReturn(user);

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"username\":\"John Doe\",\"email\":\"john.doe@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));

    }

    @Test
    void addUser_shouldReturnBadRequest_whenNameOrEmailOrPasswordIsMissing() throws Exception {
        // Given
        when(userService.createUser(any(User.class)))
                .thenThrow(new BadRequestException("Nazwa użytkownika, email i hasło są wymagane."));

        // When & Then
        mockMvc.perform(post("/api/v1/users")
                        .contentType("application/json")
                        .content("{\"name\":\"John Doe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Nazwa użytkownika, email i hasło są wymagane."))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void deleteUser_shouldReturnNoContent_whenUserExists() throws Exception {
        // Given
        Long userId = 1L;
        doNothing().when(userService).deleteUser(userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNoContent());

        verify(userService, times(1)).deleteUser(userId);
    }

    @Test
    void deleteUser_shouldReturnNotFound_whenUserDoesNotExist() throws Exception {
        // Given
        Long userId = 1L;
        doThrow(new ResourceNotFoundException("Użytkownik nie został znaleziony"))
                .when(userService).deleteUser(userId);

        // When & Then
        mockMvc.perform(delete("/api/v1/users/{id}", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Użytkownik nie został znaleziony"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUserWalletDetails_shouldReturnWalletDetails() throws Exception {
        // Given
        Long userId = 1L;
        List<Map<String, Object>> walletDetails = Collections.singletonList(
            Map.of(
                "id", 1L,
                "symbol", "AAPL",
                "name", "Apple Inc.",
                "price", 150.0,
                "amount", 10.0
            )
        );
        when(userService.getWalletDetails(userId)).thenReturn(walletDetails);

        // When & Then
        mockMvc.perform(get("/api/v1/users/{userId}/wallet/details", userId))
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
        when(userService.getWalletDetails(userId))
                .thenThrow(new ResourceNotFoundException("Użytkownik nie został znaleziony"));

        // When & Then
        mockMvc.perform(get("/api/v1/users/{userId}/wallet/details", userId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Użytkownik nie został znaleziony"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void addAssetToWallet_shouldReturnUpdatedWallet() throws Exception {
        // Given
        Long userId = 1L;
        List<Map<String, Object>> updatedWallet = Collections.singletonList(
            Map.of(
                "id", 1L,
                "symbol", "AAPL",
                "name", "Apple Inc.",
                "price", 150.0,
                "amount", 10.0
            )
        );
        when(userService.addAssetToWallet(eq(userId), any(Long.class), any(Double.class)))
            .thenReturn(updatedWallet);

        // When & Then
        mockMvc.perform(post("/api/v1/users/{userId}/wallet/add", userId)
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
        when(userService.addAssetToWallet(eq(userId), any(Long.class), any(Double.class)))
                .thenThrow(new ResourceNotFoundException("Użytkownik o ID 1 nie został znaleziony"));

        // When & Then
        mockMvc.perform(post("/api/v1/users/{userId}/wallet/add", userId)
                        .contentType("application/json")
                        .content("{\"assetId\":1,\"amount\":10.0}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Użytkownik o ID 1 nie został znaleziony"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void getUserById_shouldReturnUser() throws Exception {
        // Given
        User user = new User("John Doe", "john.doe@example.com", "password123");
        user.setId(1L);
        when(userService.getUserById(1L)).thenReturn(user);

        // When & Then
        mockMvc.perform(get("/api/v1/users/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("John Doe"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void addFunds_shouldReturnUpdatedBalance() throws Exception {
        // Given
        Map<String, Object> response = Map.of("accountBalance", 1000.0, "profit", 0.0);
        when(userService.addFunds(eq(1L), eq(1000.0))).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/v1/users/1/add-funds")
                .contentType("application/json")
                .content("{\"amount\":1000.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountBalance").value(1000.0));
    }

    @Test
    void tradeAsset_shouldReturnUpdatedWallet() throws Exception {
        // Given
        List<Map<String, Object>> wallet = Collections.singletonList(
            Map.of("id", 1L, "symbol", "AAPL", "name", "Apple Inc.", "price", 150.0, "amount", 5.0)
        );
        when(userService.tradeAsset(eq(1L), eq("BUY"), eq(1L), eq(5.0))).thenReturn(wallet);

        // When & Then
        mockMvc.perform(post("/api/v1/users/1/wallet/trade")
                .contentType("application/json")
                .content("{\"type\":\"BUY\",\"assetId\":1,\"amount\":5.0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    void getUserTransactions_shouldReturnTransactions() throws Exception {
        // Given
        List<Map<String, Object>> transactions = List.of(
            Map.of("id", 1L, "type", "BUY", "assetSymbol", "AAPL", "amount", 2.0, "price", 100.0)
        );
        when(userService.getUserTransactions(1L)).thenReturn(transactions);

        // When & Then
        mockMvc.perform(get("/api/v1/users/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("BUY"));
    }

    @Test
    void getAggregatedUserData_shouldReturnAggregatedData() throws Exception {
        // Given
        Map<String, Object> aggregated = Map.of(
            "user", Map.of("id", 1L, "username", "John Doe"),
            "transactions", List.of(),
            "assets", List.of()
        );
        when(userService.getAggregatedUserData(1L)).thenReturn(aggregated);

        // When & Then
        mockMvc.perform(get("/api/v1/users/1/aggregated"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("John Doe"));
    }
}
