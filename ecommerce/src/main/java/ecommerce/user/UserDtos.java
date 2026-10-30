package ecommerce.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class UserDtos {
    private UserDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(min = 3, max = 50)
            @Pattern(regexp = "^[A-Za-z0-9_.-]+$", message = "only letters, digits, _ . - allowed")
            String username,
            @NotBlank @Size(min = 8, max = 72, message = "password must be 8-72 characters")
            String password) {}

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}

    public record TokenResponse(String accessToken, String tokenType, long expiresInSeconds) {}

    /** لا يحتوي كلمة المرور. */
    public record UserResponse(Long id, String username, Role role) {
        public static UserResponse from(User u) { return new UserResponse(u.getId(), u.getUsername(), u.getRole()); }
    }
}
