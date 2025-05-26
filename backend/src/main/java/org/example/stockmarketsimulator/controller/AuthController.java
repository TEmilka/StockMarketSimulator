package org.example.stockmarketsimulator.controller;

import java.util.HashMap;
import java.util.Map;

import org.example.stockmarketsimulator.dto.UserRegistrationDTO;
import org.example.stockmarketsimulator.exception.BadRequestException;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.example.stockmarketsimulator.security.JwtUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtUtils jwtUtils;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
            );

            User user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new BadRequestException("User not found"));

            String token = jwtUtils.generateToken(user.getUsername());
            
            Cookie jwtCookie = new Cookie("jwt", token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400);
            response.addCookie(jwtCookie);

            return Map.of(
                "userId", user.getId().toString(),
                "role", user.getRole() 
            );
        } catch (AuthenticationException e) {
            throw new BadRequestException("Invalid credentials");
        }
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
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Użytkownik o tym emailu już istnieje.");
        }

        if (userRepository.existsByUsername(userDTO.getUsername())) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body("Użytkownik o tej nazwie użytkownika już istnieje.");
        }

        String encryptedPassword = passwordEncoder.encode(userDTO.getPassword());

        User newUser = new User();
        newUser.setUsername(userDTO.getUsername());
        newUser.setEmail(userDTO.getEmail());
        newUser.setPassword(encryptedPassword);
        newUser.setWallet(new UserWallet(newUser));

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED)
                .contentType(MediaType.APPLICATION_JSON)
                .body("Użytkownik został pomyślnie zarejestrowany.");
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
