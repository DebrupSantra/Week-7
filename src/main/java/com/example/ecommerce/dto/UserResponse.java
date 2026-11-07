package com.example.ecommerce.dto;

import com.example.ecommerce.entity.AppUser;

public record UserResponse(Long id, String email, String fullName, String role) {

    public static UserResponse from(AppUser user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getFullName(), user.getRole().name());
    }
}
