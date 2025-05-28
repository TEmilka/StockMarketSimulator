package org.example.stockmarketsimulator.controller;

import java.util.Map;

import org.example.stockmarketsimulator.dto.UserRegistrationDTO;
import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(
            summary = "Logowanie użytkownika",
            description = "Logowanie użytkownika na podstawie nazwy użytkownika i hasła. Zwraca token JWT, który należy używać do autoryzacji dalszych żądań."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Zalogowano pomyślnie, zwrócono token JWT", content = @Content(schema = @Schema(implementation = Map.class))),
            @ApiResponse(responseCode = "400", description = "Błędne dane logowania", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @PostMapping("/login")
    public Map<String, String> login(@RequestBody User request, HttpServletResponse response) {
        Map<String, String> authResult = authService.authenticate(request.getUsername(), request.getPassword());
        
        Cookie jwtCookie = new Cookie("jwt", authResult.get("token"));
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(86400);
        response.addCookie(jwtCookie);

        return Map.of(
            "userId", authResult.get("userId"),
            "role", authResult.get("role")
        );
    }

    @Operation(
            summary = "Rejestracja nowego użytkownika",
            description = "Tworzy nowego użytkownika w systemie z nazwą użytkownika, emailem i hasłem. Hasło jest szyfrowane przed zapisem."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Użytkownik zarejestrowany pomyślnie", content = @Content(schema = @Schema(implementation = String.class))),
            @ApiResponse(responseCode = "400", description = "Użytkownik o podanym emailu lub nazwie już istnieje", content = @Content),
            @ApiResponse(responseCode = "500", description = "Błąd serwera", content = @Content)
    })
    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> registerUser(@RequestBody @Valid UserRegistrationDTO userDTO) {
        try {
            authService.register(userDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Użytkownik został pomyślnie zarejestrowany.");
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(e.getMessage());
        }
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = null;
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "No refresh token provided"));
        }

        try {
            Map<String, String> refreshResult = authService.refreshToken(refreshToken);
            
            Cookie jwtCookie = new Cookie("jwt", refreshResult.get("token"));
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400);
            response.addCookie(jwtCookie);

            return ResponseEntity.ok(Map.of(
                "userId", refreshResult.get("userId"),
                "role", refreshResult.get("role")
            ));
        } catch (BadRequestException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        Cookie jwtCookie = new Cookie("jwt", null);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setSecure(true);
        jwtCookie.setPath("/");
        jwtCookie.setMaxAge(0);
        response.addCookie(jwtCookie);

        return ResponseEntity.ok().body(Map.of("message", "Logged out successfully"));
    }
}
