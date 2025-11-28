package com.example.demo.auth;


import com.example.demo.auth.config.SecurityConfig;
import com.example.demo.auth.controller.AdminUserController;
import com.example.demo.auth.entity.User;
import com.example.demo.auth.entity.enums.Role;
import com.example.demo.auth.filter.JwtAuthenticationFilter;
import com.example.demo.auth.service.AdminUserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
@WebMvcTest(controllers = AdminUserController.class)
@Import(SecurityConfig.class)
class AdminUserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AdminUserService adminUserService;

    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockitoBean
    private UserDetailsService userDetailsService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanCreateUsersThroughEndpoint() throws Exception {
        User created = new User("new-user", "hash", Role.USER);
        created.setId(42L);
        when(adminUserService.createUser(anyString(), anyString(), any())).thenReturn(created);

        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"new-user\",\"password\":\"pass\",\"role\":\"USER\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(42))
                .andExpect(jsonPath("$.username").value("new-user"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    @WithMockUser(roles = "USER")
    void nonAdminCannotCreateUsersThroughEndpoint() throws Exception {
        mockMvc.perform(post("/admin/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"hacker\",\"password\":\"pass\",\"role\":\"USER\"}"))
                .andExpect(status().isForbidden());

        verify(adminUserService, never()).createUser(anyString(), anyString(), any());
    }
}