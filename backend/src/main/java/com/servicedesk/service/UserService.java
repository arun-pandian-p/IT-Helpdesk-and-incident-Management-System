package com.servicedesk.service;

import com.servicedesk.dto.AuthDtos.LoginRequest;
import com.servicedesk.dto.AuthDtos.RegisterRequest;
import com.servicedesk.dto.AuthDtos.TokenResponse;
import com.servicedesk.dto.AuthDtos.UserDto;
import com.servicedesk.exception.AppException;
import com.servicedesk.model.AuditLog;
import com.servicedesk.model.Constants.UserRole;
import com.servicedesk.model.User;
import com.servicedesk.repository.AuditRepository;
import com.servicedesk.repository.UserRepository;
import com.servicedesk.security.JwtTokenProvider;
import com.servicedesk.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final AuditRepository auditRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       AuditRepository auditRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider tokenProvider,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.auditRepository = auditRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.auditService = auditService;
    }

    public TokenResponse login(LoginRequest req) {
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException("Invalid email or password.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new AppException("Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        if (!user.isActive()) {
            throw new AppException("User account is deactivated. Contact IT support.", HttpStatus.FORBIDDEN);
        }

        String token = tokenProvider.generateToken(user.getId(), user.getRole(), user.getEmail(), user.getName());
        auditService.logAudit(user.getId(), "USER_LOGIN", "User", user.getId(), "Logged in: " + user.getEmail());

        return new TokenResponse(token, UserDto.fromEntity(user));
    }

    @Transactional
    public TokenResponse register(RegisterRequest req) {
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new AppException("A user with this email address already exists.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        User user = new User();
        user.setId(UUID.randomUUID().toString());
        user.setName(req.getName());
        user.setEmail(req.getEmail().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setRole(UserRole.EMPLOYEE);
        user.setDepartment(req.getDepartment() != null ? req.getDepartment() : "General");
        user.setJobTitle(req.getJobTitle() != null ? req.getJobTitle() : "Staff");
        user.setPhone(req.getPhone());
        user.setLocation(req.getLocation() != null ? req.getLocation() : "Headquarters");
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);

        auditService.logAudit(user.getId(), "USER_REGISTERED", "User", user.getId(), "Registered: " + user.getEmail());
        String token = tokenProvider.generateToken(user.getId(), user.getRole(), user.getEmail(), user.getName());

        return new TokenResponse(token, UserDto.fromEntity(user));
    }

    public UserDto getMe(UserPrincipal principal) {
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new AppException("User account no longer exists.", HttpStatus.UNAUTHORIZED));
        return UserDto.fromEntity(user);
    }

    public List<UserDto> getUsers(String role, String department) {
        return userRepository.findAll(role, department).stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public List<UserDto> getSupportStaff() {
        return userRepository.findSupportStaff().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());
    }

    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found.", HttpStatus.NOT_FOUND));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto createUser(User user, String rawPassword, UserPrincipal principal) {
        requireAdmin(principal);

        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new AppException("User with email " + user.getEmail() + " already exists.", HttpStatus.BAD_REQUEST);
        }

        Instant now = Instant.now();
        user.setId(UUID.randomUUID().toString());
        user.setPasswordHash(passwordEncoder.encode(rawPassword != null ? rawPassword : "Password123!"));
        user.setActive(true);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);
        auditService.logAudit(principal.getId(), "USER_CREATED", "User", user.getId(), "Created user: " + user.getEmail());
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto updateUser(String id, User updates, UserPrincipal principal) {
        requireAdmin(principal);

        User existing = userRepository.findById(id)
                .orElseThrow(() -> new AppException("User not found.", HttpStatus.NOT_FOUND));

        if (updates.getName() != null) existing.setName(updates.getName());
        if (updates.getEmail() != null) existing.setEmail(updates.getEmail().toLowerCase());
        if (updates.getRole() != null) existing.setRole(updates.getRole());
        if (updates.getDepartment() != null) existing.setDepartment(updates.getDepartment());
        if (updates.getJobTitle() != null) existing.setJobTitle(updates.getJobTitle());
        if (updates.getPhone() != null) existing.setPhone(updates.getPhone());
        if (updates.getLocation() != null) existing.setLocation(updates.getLocation());
        existing.setActive(updates.isActive());
        existing.setUpdatedAt(Instant.now());

        userRepository.update(existing);
        return UserDto.fromEntity(existing);
    }

    public List<AuditLog> getAuditLogs(int limit) {
        return auditRepository.findRecent(limit > 0 ? limit : 50);
    }

    private void requireAdmin(UserPrincipal principal) {
        if (principal == null || !UserRole.ADMIN.equals(principal.getRole())) {
            throw new AppException("Access forbidden: requires ADMIN role.", HttpStatus.FORBIDDEN);
        }
    }
}
