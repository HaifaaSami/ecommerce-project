package ecommerce.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public final class OrderDtos {
    private OrderDtos() {}

    public record ItemRequest(@NotNull Long productId, @Min(1) @Max(1000) int quantity) {}

    /** لا يحتوي customerId ولا السعر ولا الحالة: كلها تُحدَّد من الخادم. */
    public record CreateOrderRequest(@NotEmpty @Size(max = 50) List<@Valid @NotNull ItemRequest> items) {}

    public record StatusRequest(@NotNull OrderStatus status) {}

    public record ItemResponse(Long productId, int quantity, BigDecimal unitPrice) {}

    public record OrderResponse(Long id, Long customerId, OrderStatus status, BigDecimal totalAmount,
                                Instant createdAt, List<ItemResponse> items) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getCustomerId(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt(),
                    o.getItems().stream().map(i -> new ItemResponse(i.getProductId(), i.getQuantity(), i.getUnitPrice())).toList());
        }
    }
}
