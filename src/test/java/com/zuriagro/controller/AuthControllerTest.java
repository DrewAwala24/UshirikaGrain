package com.zuriagro.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuriagro.domain.entity.Role;
import com.zuriagro.domain.entity.RoleType;
import com.zuriagro.domain.entity.User;
import com.zuriagro.dto.auth.LoginRequest;
import com.zuriagro.dto.auth.RegisterRequest;
import com.zuriagro.repository.RoleRepository;
import com.zuriagro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role farmerRole = roleRepository.save(new Role(RoleType.ROLE_FARMER, "Farmer"));

        User user = new User("farmer_grace", "grace@farm.com", passwordEncoder.encode("Secret@123"), "Grace Wambui", "0799887766");
        user.setRoles(Set.of(farmerRole));
        userRepository.save(user);
    }

    @Test
    void login_Success_ReturnsJwtToken() throws Exception {
        LoginRequest loginRequest = new LoginRequest("farmer_grace", "Secret@123");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.username").value("farmer_grace"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_FARMER"));
    }

    @Test
    void login_Failure_InvalidPassword_Returns401() throws Exception {
        LoginRequest loginRequest = new LoginRequest("farmer_grace", "WrongPassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    @Test
    void register_Success_CreatesUser() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "new_farmer", "new@farm.com", "Password@123", "New Farmer", "0711223344", null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.username").value("new_farmer"))
                .andExpect(jsonPath("$.email").value("new@farm.com"))
                .andExpect(jsonPath("$.roles[0]").value("ROLE_FARMER"));
    }

    @Test
    void register_DuplicateUsername_Returns409() throws Exception {
        RegisterRequest registerRequest = new RegisterRequest(
                "farmer_grace", "another@farm.com", "Password@123", "Grace Duplicate", "0711223344", null
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Resource Conflict"));
    }
}
