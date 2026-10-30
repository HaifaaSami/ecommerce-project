package ecommerce.notification;

import ecommerce.common.NotFoundException;
import ecommerce.notification.NotificationDtos.*;
import ecommerce.security.AuthUser;
import ecommerce.user.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications")
public class NotificationController {

    private final NotificationRepository notifications;
    private final UserRepository users;

    public NotificationController(NotificationRepository notifications, UserRepository users) {
        this.notifications = notifications;
        this.users = users;
    }

    @Operation(summary = "My notifications (each user sees only their own)")
    @GetMapping
    public Page<NotificationResponse> mine(@AuthenticationPrincipal AuthUser me, Pageable pageable) {
        return notifications.findByUserId(me.id(), pageable).map(NotificationResponse::from);
    }

    @Operation(summary = "Mark my notification as read")
    @PatchMapping("/{id}/read")
    public NotificationResponse markRead(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        Notification n = notifications.findById(id).orElseThrow(() -> new NotFoundException("Notification not found"));
        // لا نكشف وجود إشعار شخص آخر: نرجع 404 بدل 403
        if (!n.getUserId().equals(me.id())) throw new NotFoundException("Notification not found");
        n.markRead();
        return NotificationResponse.from(notifications.save(n));
    }

    @Operation(summary = "Send a notification to a user (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<NotificationResponse> send(@Valid @RequestBody SendRequest r) {
        if (!users.existsById(r.userId())) throw new NotFoundException("User not found");
        Notification saved = notifications.save(new Notification(r.userId(), r.message()));
        return ResponseEntity.status(HttpStatus.CREATED).body(NotificationResponse.from(saved));
    }
}
