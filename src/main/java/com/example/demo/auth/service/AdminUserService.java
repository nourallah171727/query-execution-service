package com.example.demo.auth.service;

import com.example.demo.auth.entity.User;
import com.example.demo.auth.entity.enums.Role;
import com.example.demo.auth.repo.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AdminUserService {

    private final UserRepository repo;
    private final PasswordEncoder encoder;

    public AdminUserService(UserRepository repo, PasswordEncoder encoder) {
        this.repo = repo;
        this.encoder = encoder;
    }

    public User createUser(String username, String password, Role role) {

        if (repo.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }

        String hash = encoder.encode(password);
        User user = new User(username, hash, role);
        return repo.save(user);
    }
}
