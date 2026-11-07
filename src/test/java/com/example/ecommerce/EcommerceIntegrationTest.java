package com.example.ecommerce;

import com.example.ecommerce.dto.CartItemRequest;
import com.example.ecommerce.dto.PaymentRequest;
import com.example.ecommerce.dto.ProductRequest;
import com.example.ecommerce.dto.RegisterRequest;
import com.example.ecommerce.entity.PaymentMethod;
import com.example.ecommerce.entity.PaymentStatus;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.repository.PaymentRepository;
import com.example.ecommerce.repository.ProductRepository;
import com.example.ecommerce.service.CartService;
import com.example.ecommerce.service.CategoryService;
import com.example.ecommerce.service.OrderService;
import com.example.ecommerce.service.PaymentService;
import com.example.ecommerce.service.ProductService;
import com.example.ecommerce.service.UserService;
import org.flywaydb.core.Flyway;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class EcommerceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private CategoryService categoryService;

    @Autowired
    private ProductService productService;

    @Autowired
    private CartService cartService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MockMvc mockMvc;

    @Test
    void migrationsSeedCatalogAndCheckoutCanBeCancelledAndRetriedSafely() {
        assertEquals(2, flyway.info().applied().length);

        var user = userService.register(new RegisterRequest(
                "buyer@example.com", "a-long-test-password", "Test Buyer"));
        var category = categoryService.findAll().get(0);
        var product = productService.create(new ProductRequest(
                "TEST-001", "Test product", "Integration test product",
                new BigDecimal("12.50"), 8, true, Set.of(category.getId())));

        cartService.addItem(user.getEmail(), new CartItemRequest(product.getId(), 2));
        var order = orderService.checkout(user.getEmail());

        assertEquals(new BigDecimal("25.00"), order.total());
        assertEquals(6, productRepository.findById(product.getId()).orElseThrow().getInventoryQuantity());

        var request = new PaymentRequest(PaymentMethod.CARD_PROVIDER, "test-idempotency-key-001");
        var firstPayment = paymentService.initiate(user.getEmail(), order.id(), request);
        var retry = paymentService.initiate(user.getEmail(), order.id(), request);

        assertEquals(firstPayment.id(), retry.id());
        assertEquals("PENDING", retry.status());

        orderService.cancel(user.getEmail(), order.id());

        assertEquals(8, productRepository.findById(product.getId()).orElseThrow().getInventoryQuantity());
        assertEquals(PaymentStatus.CANCELLED, paymentRepository
                .findById(firstPayment.id()).orElseThrow().getStatus());
        assertEquals("CANCELLED", orderService.findById(user.getEmail(), order.id()).status());
    }

    @Test
    void failedCheckoutRollsBackInventoryAndKeepsCart() {
        var user = userService.register(new RegisterRequest(
                "rollback@example.com", "another-test-password", "Rollback Buyer"));
        var category = categoryService.findAll().get(0);
        var product = productService.create(new ProductRequest(
                "TEST-002", "Limited product", null,
                new BigDecimal("8.00"), 5, true, Set.of(category.getId())));

        cartService.addItem(user.getEmail(), new CartItemRequest(product.getId(), 3));
        var managedProduct = productRepository.findById(product.getId()).orElseThrow();
        managedProduct.update(managedProduct.getSku(), managedProduct.getName(), managedProduct.getDescription(),
                managedProduct.getPrice(), 2, true, managedProduct.getCategories());

        assertThrows(ConflictException.class, () -> orderService.checkout(user.getEmail()));

        assertEquals(2, productRepository.findById(product.getId()).orElseThrow().getInventoryQuantity());
        assertEquals(3, cartService.getCart(user.getEmail()).items().get(0).quantity());
    }

    @Test
    void registrationAuthenticationAndCatalogAuthorizationWorkOverHttp() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/users/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"http-buyer@example.com","password":"a-long-http-test-password","fullName":"HTTP Buyer"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("CUSTOMER"));

        mockMvc.perform(get("/api/users/me")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                                "http-buyer@example.com", "a-long-http-test-password")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("http-buyer@example.com"));

        mockMvc.perform(get("/api/products").param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(Matchers.greaterThanOrEqualTo(3)));

        mockMvc.perform(post("/api/admin/products")
                        .with(SecurityMockMvcRequestPostProcessors.httpBasic(
                                "http-buyer@example.com", "a-long-http-test-password"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sku":"DENIED-001","name":"Denied","price":1.00,"inventoryQuantity":1,"active":true,"categoryIds":[]}
                                """))
                .andExpect(status().isForbidden());
    }
}
