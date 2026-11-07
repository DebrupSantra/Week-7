package com.example.ecommerce.repository;

import com.example.ecommerce.entity.Category;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @EntityGraph(attributePaths = "parent")
    List<Category> findAllByOrderByNameAsc();

    @Override
    @EntityGraph(attributePaths = "parent")
    Optional<Category> findById(Long id);

    boolean existsByNameIgnoreCase(String name);
}
