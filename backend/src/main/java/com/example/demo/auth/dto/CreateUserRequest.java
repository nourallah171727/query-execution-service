package com.example.demo.auth.dto;

import com.example.demo.auth.entity.enums.Role;

    public record CreateUserRequest(
            String username,
            String password,
            Role role
    ) {}
