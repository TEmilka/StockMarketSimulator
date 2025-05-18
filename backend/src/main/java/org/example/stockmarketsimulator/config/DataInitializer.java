package org.example.stockmarketsimulator.config;

import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.model.UserWallet;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (!userRepository.existsByUsername("admin")) {
            User admin = new User();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin"));
            admin.setEmail("admin@example.com");
            admin.setRole("ROLE_ADMIN");
            admin.setWallet(new UserWallet(admin));
            userRepository.save(admin);
        }
    }
}
