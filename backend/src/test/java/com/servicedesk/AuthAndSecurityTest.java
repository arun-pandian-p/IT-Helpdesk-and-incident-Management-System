package com.servicedesk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servicedesk.dto.AuthDtos.LoginRequest;
import com.servicedesk.dto.AuthDtos.RegisterRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthAndSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Health check should return 200 OK")
    void testHealthCheck() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("ok")))
                .andExpect(jsonPath("$.version", is("1.0.0")));
    }

    @Test
    @DisplayName("Login with valid credentials should succeed and return JWT token")
    void testLoginSuccess() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("employee@example.com");
        req.setPassword("Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.email", is("employee@example.com")))
                .andExpect(jsonPath("$.user.role", is("EMPLOYEE")));
    }

    @Test
    @DisplayName("Login with invalid credentials should fail with 401 Unauthorized")
    void testLoginInvalid() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("employee@example.com");
        req.setPassword("WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail", containsString("Invalid email or password")));
    }

    @Test
    @DisplayName("Accessing protected /api/auth/me without token should return 401 Unauthorized")
    void testProtectedWithoutToken() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Registration of a new employee should return 201 Created")
    void testRegisterNewEmployee() throws Exception {
        RegisterRequest req = new RegisterRequest();
        req.setName("Test User");
        req.setEmail("newuser_" + System.currentTimeMillis() + "@example.com");
        req.setPassword("SecurePass123!");
        req.setDepartment("Finance");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.user.name", is("Test User")))
                .andExpect(jsonPath("$.user.role", is("EMPLOYEE")));
    }
}
