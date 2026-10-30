package ecommerce.user;

import ecommerce.common.NotFoundException;
import ecommerce.security.AuthUser;
import ecommerce.user.UserDtos.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users")
public class UserController {

    private final UserRepository users;

    public UserController(UserRepository users) { this.users = users; }

    @Operation(summary = "Current user profile")
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AuthUser me) {
        return users.findById(me.id()).map(UserResponse::from)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    @Operation(summary = "List users (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<UserResponse> list(Pageable pageable) {
        return users.findAll(pageable).map(UserResponse::from);
    }
}
