package com.example.ecommerce.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product extends AuditedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 80)
    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @Size(max = 4000)
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotNull
    @PositiveOrZero
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @PositiveOrZero
    @Column(name = "inventory_quantity", nullable = false)
    private int inventoryQuantity;

    @Column(nullable = false)
    private boolean active = true;

    @Version
    private long version;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_categories",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "category_id"))
    @BatchSize(size = 50)
    private Set<Category> categories = new LinkedHashSet<>();

    protected Product() {
    }

    public Product(String sku, String name, String description, BigDecimal price, int inventoryQuantity) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.inventoryQuantity = inventoryQuantity;
    }

    public Long getId() {
        return id;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public int getInventoryQuantity() {
        return inventoryQuantity;
    }

    public boolean isActive() {
        return active;
    }

    public Set<Category> getCategories() {
        return categories;
    }

    public void update(String sku, String name, String description, BigDecimal price,
                       int inventoryQuantity, boolean active, Set<Category> categories) {
        this.sku = sku;
        this.name = name;
        this.description = description;
        this.price = price;
        this.inventoryQuantity = inventoryQuantity;
        this.active = active;
        this.categories = categories;
    }

    public void reserve(int quantity) {
        if (quantity > inventoryQuantity) {
            throw new IllegalStateException("Insufficient inventory for product " + sku);
        }
        inventoryQuantity -= quantity;
    }

    public void restock(int quantity) {
        inventoryQuantity = Math.addExact(inventoryQuantity, quantity);
    }
}
