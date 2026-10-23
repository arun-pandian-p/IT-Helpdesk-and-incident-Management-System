package com.servicedesk.controller;

import com.servicedesk.dto.AuthDtos.UserDto;
import com.servicedesk.model.AuditLog;
import com.servicedesk.model.User;
import com.servicedesk.security.UserPrincipal;
import com.servicedesk.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getUsers(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String department) {
        return ResponseEntity.ok(userService.getUsers(role, department));
    }

    @GetMapping("/support-staff")
    public ResponseEntity<List<UserDto>> getSupportStaff() {
        return ResponseEntity.ok(userService.getSupportStaff());
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserDto> getUser(@PathVariable String id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PostMapping
    public ResponseEntity<UserDto> createUser(
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = new User();
        user.setName((String) payload.get("name"));
        user.setEmail((String) payload.get("email"));
        user.setRole((String) payload.getOrDefault("role", "EMPLOYEE"));
        user.setDepartment((String) payload.getOrDefault("department", "General"));
        user.setJobTitle((String) payload.get("job_title"));
        user.setPhone((String) payload.get("phone"));
        user.setLocation((String) payload.getOrDefault("location", "Headquarters"));
        String password = (String) payload.get("password");

        UserDto created = userService.createUser(user, password, principal);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<UserDto> updateUser(
            @PathVariable String id,
            @RequestBody Map<String, Object> payload,
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = new User();
        if (payload.containsKey("name")) user.setName((String) payload.get("name"));
        if (payload.containsKey("email")) user.setEmail((String) payload.get("email"));
        if (payload.containsKey("role")) user.setRole((String) payload.get("role"));
        if (payload.containsKey("department")) user.setDepartment((String) payload.get("department"));
        if (payload.containsKey("job_title")) user.setJobTitle((String) payload.get("job_title"));
        if (payload.containsKey("phone")) user.setPhone((String) payload.get("phone"));
        if (payload.containsKey("location")) user.setLocation((String) payload.get("location"));
        if (payload.containsKey("is_active")) user.setActive(Boolean.TRUE.equals(payload.get("is_active")));

        UserDto updated = userService.updateUser(id, user, principal);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/audit/logs")
    public ResponseEntity<List<AuditLog>> getAuditLogs(
            @RequestParam(defaultValue = "50") int limit) {
        return ResponseEntity.ok(userService.getAuditLogs(limit));
    }
}
