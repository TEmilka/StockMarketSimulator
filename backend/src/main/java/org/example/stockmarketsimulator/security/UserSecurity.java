package org.example.stockmarketsimulator.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {
    
    @Autowired
    private JwtUtils jwtUtils;
    
    public boolean isCurrentUser(String userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        try {
            String currentUserId = JwtUtils.getCurrentUserId(authentication);
            return userId != null && userId.equals(currentUserId);
        } catch (Exception e) {
            return false;
        }
    }
}
