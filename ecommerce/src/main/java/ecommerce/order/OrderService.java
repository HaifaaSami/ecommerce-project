package ecommerce.order;

import ecommerce.common.ConflictException;
import ecommerce.common.NotFoundException;
import ecommerce.notification.Notification;
import ecommerce.notification.NotificationRepository;
import ecommerce.order.OrderDtos.*;
import ecommerce.product.Product;
import ecommerce.product.ProductRepository;
import ecommerce.security.AuthUser;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final ProductRepository products;
    private final NotificationRepository notifications;

    public OrderService(OrderRepository orders, ProductRepository products, NotificationRepository notifications) {
        this.orders = orders;
        this.products = products;
        this.notifications = notifications;
    }

    @Transactional
    public Order create(AuthUser me, CreateOrderRequest req) {
        List<OrderItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;
        for (ItemRequest it : req.items()) {
            Product p = products.findById(it.productId())
                    .orElseThrow(() -> new NotFoundException("Product " + it.productId() + " not found"));
            items.add(new OrderItem(p.getId(), it.quantity(), p.getPrice()));
            total = total.add(p.getPrice().multiply(BigDecimal.valueOf(it.quantity())));
        }
        Order saved = orders.save(new Order(me.id(), items, total));
        notifications.save(new Notification(me.id(), "Your order #" + saved.getId() + " was received (PENDING)."));
        return saved;
    }

    /** صاحب الطلب أو المسؤول فقط، غير ذلك => 403. */
    @Transactional(readOnly = true)
    public Order getAuthorized(Long id, AuthUser me) {
        Order o = orders.findById(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
        if (!me.isAdmin() && !o.getCustomerId().equals(me.id())) {
            throw new AccessDeniedException("Not your order");
        }
        return o;
    }

    @Transactional(readOnly = true)
    public Page<Order> mine(AuthUser me, Pageable pageable) {
        return orders.findByCustomerId(me.id(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<Order> all(Pageable pageable) { return orders.findAll(pageable); }

    @Transactional
    public Order changeStatus(Long id, OrderStatus status) {
        Order o = orders.findById(id).orElseThrow(() -> new NotFoundException("Order " + id + " not found"));
        if (o.getStatus() == OrderStatus.DELIVERED || o.getStatus() == OrderStatus.CANCELLED) {
            throw new ConflictException("Order is already " + o.getStatus() + " and can no longer change");
        }
        if (o.getStatus() == status) {
            throw new ConflictException("Order is already " + status);
        }
        o.setStatus(status);
        Order saved = orders.save(o);
        notifications.save(new Notification(o.getCustomerId(), "Your order #" + id + " is now " + status + "."));
        return saved;
    }
}
