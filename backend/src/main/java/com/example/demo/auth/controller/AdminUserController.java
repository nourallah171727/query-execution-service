package com.example.demo.auth.controller;

import com.example.demo.auth.dto.CreateUserRequest;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.service.AdminUserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final AdminUserService service;

    public AdminUserController(AdminUserService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Object> createUser(@RequestBody CreateUserRequest req) {

        User created = service.createUser(
                req.username(),
                req.password(),
                req.role()
        );

        return Map.of(
                "id", created.getId(),
                "username", created.getUsername(),
                "role", created.getRole()
        );
    }
}