package org.example.stockmarketsimulator.security;

import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InMemoryUserDetailsManager inMemoryManager;

    public CustomUserDetailsService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;

        // Tworzymy statycznych użytkowników
        var admin = org.springframework.security.core.userdetails.User.builder()
                .username("admin")
                .password(passwordEncoder.encode("admin"))
                .roles("ADMIN")
                .build();

        var user = org.springframework.security.core.userdetails.User.builder()
                .username("user")
                .password(passwordEncoder.encode("user"))
                .roles("USER")
                .build();

        this.inMemoryManager = new InMemoryUserDetailsManager(admin, user);
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Najpierw sprawdzamy statycznych użytkowników
        if (username.equals("admin") || username.equals("user")) {
            return inMemoryManager.loadUserByUsername(username);
        }

        // Jeśli to nie statyczny użytkownik, szukamy w bazie danych
        return userRepository.findByUsername(username)
                .map(user -> new org.springframework.security.core.userdetails.User(
                        user.getUsername(),
                        user.getPassword(),
                        Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                ))
                .orElseThrow(() -> new UsernameNotFoundException("Użytkownik nie znaleziony: " + username));
    }
}