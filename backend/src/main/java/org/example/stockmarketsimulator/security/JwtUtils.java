package org.example.stockmarketsimulator.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.example.stockmarketsimulator.model.User;
import org.example.stockmarketsimulator.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

//Klasa odpowiedzialna za generowanie, walidację i dekodowanie tokenów JWT
@Component
public class JwtUtils {

    private static final String SECRET_KEY = "TwojeSuperTajneHasloDoJWT_1234567890"; // powinno być min 256 bitów!

    private static final long EXPIRATION_TIME = 86400000; // 1 dzień w ms

    private final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    @Autowired
    private UserRepository userRepository;

    public String generateToken(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
                
        return Jwts.builder()
                .setSubject(username)
                .claim("userId", user.getId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
