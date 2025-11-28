package com.example.demo.service;

import com.example.demo.auth.entity.User;
import com.example.demo.auth.entity.enums.Role;
import com.example.demo.auth.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuerySecurityServiceTest {

    @Mock
    private AuthService authService;

    private QuerySecurityService securityService;

    @BeforeEach
    void setUp() {
        securityService = new QuerySecurityService(authService);
    }

    @Test
    void nonAdminUserCannotExecuteWriteQueries() {
        User user = new User("alice", "hash", Role.USER);
        when(authService.getCurrentUser()).thenReturn(user);

        assertThrows(IllegalArgumentException.class,
                () -> securityService.enforcePermission("INSERT INTO table VALUES (1)"));
    }

    @Test
    void adminCanExecuteWriteQueries() {
        User admin = new User("admin", "hash", Role.ADMIN);
        when(authService.getCurrentUser()).thenReturn(admin);

        assertDoesNotThrow(() -> securityService.enforcePermission("UPDATE passengers SET name = 'A'"));
    }

    @Test
    void nonAdminUserCanExecuteReadQueries() {
        User user = new User("bob", "hash", Role.USER);
        when(authService.getCurrentUser()).thenReturn(user);

        assertDoesNotThrow(() -> securityService.enforcePermission("SELECT * FROM dataset"));
    }
}