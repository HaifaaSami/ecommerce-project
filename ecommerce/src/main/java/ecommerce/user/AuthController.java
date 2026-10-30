package ecommerce.user;

import ecommerce.common.ConflictException;
import ecommerce.security.JwtService;
import ecommerce.security.LoginAttemptService;
import ecommerce.user.UserDtos.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth")
public class AuthController {

    // hash وهمي لتساوي زمن الاستجابة عند عدم وجود المستخدم (يقلل تسريب وجود الحساب)
    private final String dummyHash;

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final LoginAttemptService attempts;

    public AuthController(UserRepository users, PasswordEncoder encoder, JwtService jwt, LoginAttemptService attempts) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.attempts = attempts;
        this.dummyHash = encoder.encode("not-a-real-password");
    }

    @Operation(summary = "Register a new customer (role is always CUSTOMER)")
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest req) {
        if (users.existsByUsername(req.username())) {
            throw new ConflictException("Username already taken");
        }
        try {
            User saved = users.save(new User(req.username(), encoder.encode(req.password()), Role.CUSTOMER));
            return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(saved));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Username already taken");
        }
    }

    @Operation(summary = "Login and receive a JWT")
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest req, HttpServletRequest http) {
        String ip = http.getRemoteAddr();
        attempts.checkAllowed(req.username(), ip);

        User user = users.findByUsername(req.username()).orElse(null);
        boolean ok = encoder.matches(req.password(), user != null ? user.getPassword() : dummyHash) && user != null;
        if (!ok) {
            attempts.recordFailure(req.username(), ip);
            throw new BadCredentialsException("Invalid credentials");
        }
        attempts.recordSuccess(req.username(), ip);
        String token = jwt.generate(user.getId(), user.getUsername(), user.getRole().name());
        return new TokenResponse(token, "Bearer", jwt.ttlSeconds());
    }
}
