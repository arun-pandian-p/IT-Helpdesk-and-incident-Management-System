package com.servicedesk.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.servicedesk.model.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AuthDtos {

    public static class LoginRequest {
        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address format")
        private String email;

        @NotBlank(message = "Password is required")
        private String password;

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }

    public static class RegisterRequest {
        @NotBlank(message = "Name is required")
        private String name;

        @NotBlank(message = "Email is required")
        @Email(message = "Invalid email address format")
        private String email;

        @NotBlank(message = "Password is required")
        @Size(min = 6, message = "Password must be at least 6 characters")
        private String password;

        private String department = "General";
        private String jobTitle = "Staff";
        private String phone;
        private String location = "Headquarters";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }

        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getDepartment() { return department; }
        public void setDepartment(String department) { this.department = department; }

        public String getJobTitle() { return jobTitle; }
        public void setJobTitle(String jobTitle) { this.jobTitle = jobTitle; }

        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }

        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
    }

    public static class UserDto {
        private String id;
        private String name;
        private String email;
        private String role;
        private String department;
        @JsonProperty("job_title")
        private String jobTitle;
        private String phone;
        private String location;
        @JsonProperty("is_active")
        private boolean active;
        @JsonProperty("created_at")
        private String createdAt;
        @JsonProperty("updated_at")
        private String updatedAt;

        public static UserDto fromEntity(User user) {
            if (user == null) return null;
            UserDto dto = new UserDto();
            dto.id = user.getId();
            dto.name = user.getName();
            dto.email = user.getEmail();
            dto.role = user.getRole();
            dto.department = user.getDepartment();
            dto.jobTitle = user.getJobTitle();
            dto.phone = user.getPhone();
            dto.location = user.getLocation();
            dto.active = user.isActive();
            dto.createdAt = user.getCreatedAt() != null ? user.getCreatedAt().toString() : null;
            dto.updatedAt = user.getUpdatedAt() != null ? user.getUpdatedAt().toString() : null;
            return dto;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getEmail() { return email; }
        public String getRole() { return role; }
        public String getDepartment() { return department; }
        public String getJobTitle() { return jobTitle; }
        public String getPhone() { return phone; }
        public String getLocation() { return location; }
        public boolean isActive() { return active; }
        public String getCreatedAt() { return createdAt; }
        public String getUpdatedAt() { return updatedAt; }
    }

    public static class TokenResponse {
        @JsonProperty("access_token")
        private String accessToken;
        private UserDto user;

        public TokenResponse(String accessToken, UserDto user) {
            this.accessToken = accessToken;
            this.user = user;
        }

        public String getAccessToken() { return accessToken; }
        public UserDto getUser() { return user; }
    }
}
