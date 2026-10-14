package com.zuriagro.dto.auth;

import java.util.List;

public record JwtResponse(
        String accessToken,
        String tokenType,
        Long id,
        String username,
        String email,
        String fullName,
        List<String> roles
) {
    public JwtResponse(String accessToken, Long id, String username, String email, String fullName, List<String> roles) {
        this(accessToken, "Bearer", id, username, email, fullName, roles);
    }
}
