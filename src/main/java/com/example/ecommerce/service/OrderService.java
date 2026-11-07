package com.example.ecommerce.service;

import com.example.ecommerce.dto.OrderResponse;
import com.example.ecommerce.dto.PageResponse;
import com.example.ecommerce.entity.AppUser;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.OrderItem;
import com.example.ecommerce.entity.OrderStatus;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.PurchaseOrder;
import com.example.ecommerce.entity.ShoppingCart;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.OrderRepository;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ShoppingCartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;

@Service
@Transactional
public class OrderService {

    private final UserService userService;
    private final ShoppingCartRepository cartRepository;
    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public OrderService(UserService userService, ShoppingCartRepository cartRepository,
                        ProductRepository productRepository, OrderRepository orderRepository,
                        PaymentRepository paymentRepository) {
        this.userService = userService;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public OrderResponse checkout(String email) {
        AppUser user = userService.requireByEmail(email);
        ShoppingCart cart = cartRepository.findLockedByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart was not found"));
        if (cart.getItems().isEmpty()) {
            throw new BadRequestException("Cannot check out an empty cart");
        }

        PurchaseOrder order = new PurchaseOrder(user, BigDecimal.ZERO);
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem cartItem : cart.getItems().stream()
                .sorted(Comparator.comparing(item -> item.getProduct().getId())).toList()) {
            Product product = productRepository.findLockedById(cartItem.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("A cart product was not found"));
            if (!product.isActive()) {
                throw new ConflictException("Product " + product.getSku() + " is no longer available");
            }
            if (cartItem.getQuantity() > product.getInventoryQuantity()) {
                throw new ConflictException("Insufficient inventory for product " + product.getSku());
            }
            product.reserve(cartItem.getQuantity());
            order.addItem(new OrderItem(product, cartItem.getQuantity(), product.getPrice()));
            total = total.add(product.getPrice().multiply(BigDecimal.valueOf(cartItem.getQuantity())));
        }
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING_PAYMENT);
        order = orderRepository.saveAndFlush(order);
        cart.clear();
        return OrderResponse.from(order);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderResponse> findMyOrders(String email, Pageable pageable) {
        AppUser user = userService.requireByEmail(email);
        Page<OrderResponse> orders = orderRepository.findByUserId(user.getId(), pageable)
                .map(OrderResponse::from);
        return PageResponse.from(orders);
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(String email, Long orderId) {
        AppUser user = userService.requireByEmail(email);
        return orderRepository.findByIdAndUserId(orderId, user.getId())
                .map(OrderResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found"));
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public OrderResponse cancel(String email, Long orderId) {
        AppUser user = userService.requireByEmail(email);
        PurchaseOrder order = orderRepository.findLockedByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " was not found"));
        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new ConflictException("Only orders awaiting payment can be cancelled");
        }
        for (OrderItem item : order.getItems()) {
            Product product = productRepository.findLockedById(item.getProduct().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("An order product was not found"));
            product.restock(item.getQuantity());
        }
        order.setStatus(OrderStatus.CANCELLED);
        paymentRepository.findByOrderId(orderId).stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PENDING)
                .forEach(payment -> payment.setStatus(PaymentStatus.CANCELLED));
        return OrderResponse.from(order);
    }
}
