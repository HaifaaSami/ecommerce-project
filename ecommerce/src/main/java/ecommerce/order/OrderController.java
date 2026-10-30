package ecommerce.order;

import ecommerce.order.OrderDtos.*;
import ecommerce.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
@Tag(name = "Orders")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) { this.service = service; }

    @Operation(summary = "Create an order for the authenticated customer")
    @PostMapping
    public ResponseEntity<OrderResponse> create(@Valid @RequestBody CreateOrderRequest req,
                                                @AuthenticationPrincipal AuthUser me) {
        Order o = service.create(me, req);
        return ResponseEntity.created(URI.create("/api/v1/orders/" + o.getId())).body(OrderResponse.from(o));
    }

    @Operation(summary = "My orders (paginated)")
    @GetMapping("/my")
    public Page<OrderResponse> my(@AuthenticationPrincipal AuthUser me, Pageable pageable) {
        return service.mine(me, pageable).map(OrderResponse::from);
    }

    @Operation(summary = "Order details (owner or ADMIN)")
    @GetMapping("/{id}")
    public OrderResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthUser me) {
        return OrderResponse.from(service.getAuthorized(id, me));
    }

    @Operation(summary = "All orders (ADMIN only, paginated)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public Page<OrderResponse> all(Pageable pageable) {
        return service.all(pageable).map(OrderResponse::from);
    }

    @Operation(summary = "Change order status (ADMIN only)")
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{id}/status")
    public OrderResponse changeStatus(@PathVariable Long id, @Valid @RequestBody StatusRequest req) {
        return OrderResponse.from(service.changeStatus(id, req.status()));
    }
}
