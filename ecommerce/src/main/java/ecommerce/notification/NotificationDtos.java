package ecommerce.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class NotificationDtos {
    private NotificationDtos() {}

    public record SendRequest(@NotNull Long userId, @NotBlank @Size(max = 500) String message) {}

    public record NotificationResponse(Long id, Long userId, String message, boolean read, Instant createdAt) {
        public static NotificationResponse from(Notification n) {
            return new NotificationResponse(n.getId(), n.getUserId(), n.getMessage(), n.isRead(), n.getCreatedAt());
        }
    }
}
