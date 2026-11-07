package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Product;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "categories")
    @Query("select distinct p from Product p where p.id = :id")
    Optional<Product> findDetailedById(@Param("id") Long id);

    @Query("select distinct p from Product p where p.active = true "
            + "and (:categoryId is null or exists "
            + "(select c.id from p.categories c where c.id = :categoryId)) "
            + "and (:search is null or lower(p.name) like lower(concat('%', :search, '%')) "
            + "or lower(p.sku) like lower(concat('%', :search, '%')))")
    Page<Product> searchCatalog(
            @Param("categoryId") Long categoryId,
            @Param("search") String search,
            Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Product p where p.id = :id")
    Optional<Product> findLockedById(@Param("id") Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
}
