package com.example.ecommerce.repository;

import com.example.ecommerce.entity.ShoppingCart;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ShoppingCart c where c.user.id = :userId")
    Optional<ShoppingCart> findLockedByUserId(@Param("userId") Long userId);

    @Query("select c from ShoppingCart c where c.user.id = :userId")
    Optional<ShoppingCart> findByUserId(@Param("userId") Long userId);
}
