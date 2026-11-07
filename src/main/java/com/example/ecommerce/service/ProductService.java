package com.example.ecommerce.service;

import com.example.ecommerce.dto.ProductRequest;
import com.example.ecommerce.dto.ProductResponse;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.entity.Product;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import com.example.ecommerce.repository.ProductRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> search(Long categoryId, String search, Pageable pageable) {
        return productRepository.searchCatalog(categoryId, normalize(search), pageable)
                .map(ProductResponse::from);
    }

    @Cacheable(cacheNames = "products", key = "#id")
    @Transactional(readOnly = true)
    public Product findById(Long id) {
        Product product = productRepository.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
        if (!product.isActive()) {
            throw new ResourceNotFoundException("Product " + id + " was not found");
        }
        return product;
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public Product create(ProductRequest request) {
        if (productRepository.existsBySkuIgnoreCase(request.sku().trim())) {
            throw new ConflictException("A product with that SKU already exists");
        }
        Product product = new Product(request.sku().trim(), request.name().trim(), request.description(),
                request.price(), request.inventoryQuantity());
        product.update(product.getSku(), product.getName(), product.getDescription(), product.getPrice(),
                product.getInventoryQuantity(), request.active(), resolveCategories(request.categoryIds()));
        return productRepository.save(product);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public Product update(Long id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found"));
        if (productRepository.existsBySkuIgnoreCaseAndIdNot(request.sku().trim(), id)) {
            throw new ConflictException("A product with that SKU already exists");
        }
        product.update(request.sku().trim(), request.name().trim(), request.description(),
                request.price(), request.inventoryQuantity(), request.active(),
                resolveCategories(request.categoryIds()));
        return productRepository.save(product);
    }

    @CacheEvict(cacheNames = "products", allEntries = true)
    public void delete(Long id) {
        productRepository.delete(productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product " + id + " was not found")));
    }

    private Set<Category> resolveCategories(Set<Long> ids) {
        Set<Category> categories = new HashSet<>();
        for (Long id : ids) {
            categories.add(categoryRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " was not found")));
        }
        return categories;
    }

    private String normalize(String search) {
        return search == null || search.isBlank() ? null : search.trim();
    }
}
