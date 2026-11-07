package com.example.ecommerce.service;

import com.example.ecommerce.dto.CartResponse;
import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.QuantityRequest;
import com.example.ecommerce.entity.AppUser;
import com.example.ecommerce.entity.CartItem;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.entity.ShoppingCart;
import com.example.ecommerce.exception.BadRequestException;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.repository.ShoppingCartRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CartService {

    private static final int MAX_LINE_QUANTITY = 1000;

    private final UserService userService;
    private final ShoppingCartRepository cartRepository;
    private final ProductRepository productRepository;

    public CartService(UserService userService, ShoppingCartRepository cartRepository,
                       ProductRepository productRepository) {
        this.userService = userService;
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
    }

    public CartResponse getCart(String email) {
        AppUser user = userService.requireByEmail(email);
        return CartResponse.from(requireCart(user.getId()));
    }

    public CartResponse addItem(String email, CartItemRequest request) {
        AppUser user = userService.requireByEmail(email);
        ShoppingCart cart = requireLockedCart(user.getId());
        Product product = productRepository.findById(request.productId())
                .filter(Product::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + request.productId() + " was not found"));
        CartItem existing = cart.getItems().stream()
                .filter(item -> item.getProduct().getId().equals(product.getId()))
                .findFirst().orElse(null);
        int quantity = request.quantity() + (existing == null ? 0 : existing.getQuantity());
        validateQuantity(quantity);
        if (quantity > product.getInventoryQuantity()) {
            throw new ConflictException("Requested quantity exceeds available inventory");
        }
        if (existing == null) {
            cart.addItem(new CartItem(product, quantity));
        } else {
            existing.setQuantity(quantity);
        }
        return CartResponse.from(cartRepository.save(cart));
    }

    public CartResponse updateItem(String email, Long itemId, QuantityRequest request) {
        AppUser user = userService.requireByEmail(email);
        ShoppingCart cart = requireLockedCart(user.getId());
        CartItem item = cart.getItems().stream().filter(candidate -> candidate.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Cart item " + itemId + " was not found"));
        validateQuantity(request.quantity());
        if (request.quantity() > item.getProduct().getInventoryQuantity()) {
            throw new ConflictException("Requested quantity exceeds available inventory");
        }
        item.setQuantity(request.quantity());
        return CartResponse.from(cartRepository.save(cart));
    }

    public CartResponse removeItem(String email, Long itemId) {
        AppUser user = userService.requireByEmail(email);
        ShoppingCart cart = requireLockedCart(user.getId());
        CartItem item = cart.getItems().stream().filter(candidate -> candidate.getId().equals(itemId))
                .findFirst().orElseThrow(() -> new ResourceNotFoundException("Cart item " + itemId + " was not found"));
        cart.removeItem(item);
        return CartResponse.from(cartRepository.save(cart));
    }

    public CartResponse clear(String email) {
        AppUser user = userService.requireByEmail(email);
        ShoppingCart cart = requireLockedCart(user.getId());
        cart.clear();
        return CartResponse.from(cartRepository.save(cart));
    }

    private ShoppingCart requireCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart was not found"));
    }

    private ShoppingCart requireLockedCart(Long userId) {
        return cartRepository.findLockedByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Shopping cart was not found"));
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1 || quantity > MAX_LINE_QUANTITY) {
            throw new BadRequestException("Quantity must be between 1 and " + MAX_LINE_QUANTITY);
        }
    }
}
