package com.zuriagro.dto.auth;

import com.zuriagro.domain.entity.Role;
import com.zuriagro.domain.entity.User;

import java.time.Instant;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        String phoneNumber,
        boolean enabled,
        List<String> roles,
        Instant createdAt
) {
    public static UserResponse fromEntity(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getFullName(),
                user.getPhoneNumber(),
                user.isEnabled(),
                user.getRoles().stream().map(role -> role.getName().name()).toList(),
                user.getCreatedAt()
        );
    }
}
