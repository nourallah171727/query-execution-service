package com.example.demo.service;

import com.example.demo.auth.entity.User;
import com.example.demo.auth.entity.enums.Role;
import com.example.demo.auth.service.AuthService;
import com.example.demo.util.QueryUtil;
import org.springframework.stereotype.Service;

@Service
public class QuerySecurityService {

    private final AuthService authService;

    public QuerySecurityService(AuthService authService) {
        this.authService = authService;
    }

    public void enforcePermission(String sql) {
        User user = authService.getCurrentUser();

        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isAdmin && QueryUtil.isWriteQuery(sql)) {
            throw new IllegalArgumentException("You cannot execute write queries");
        }
    }
}