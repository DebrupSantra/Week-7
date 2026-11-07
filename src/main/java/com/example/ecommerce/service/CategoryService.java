package com.example.ecommerce.service;

import com.example.ecommerce.dto.CategoryRequest;
import com.example.ecommerce.entity.Category;
import com.example.ecommerce.exception.ConflictException;
import com.example.ecommerce.exception.ResourceNotFoundException;
import com.example.ecommerce.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public List<Category> findAll() {
        return categoryRepository.findAllByOrderByNameAsc();
    }

    @Transactional(readOnly = true)
    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category " + id + " was not found"));
    }

    public Category create(CategoryRequest request) {
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A category with that name already exists");
        }
        return categoryRepository.save(new Category(name, request.description(), resolveParent(null, request.parentId())));
    }

    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);
        String name = request.name().trim();
        if (categoryRepository.existsByNameIgnoreCase(name) && !category.getName().equalsIgnoreCase(name)) {
            throw new ConflictException("A category with that name already exists");
        }
        category.update(name, request.description(), resolveParent(id, request.parentId()));
        return categoryRepository.save(category);
    }

    public void delete(Long id) {
        categoryRepository.delete(findById(id));
    }

    private Category resolveParent(Long categoryId, Long parentId) {
        if (parentId == null) {
            return null;
        }
        if (parentId.equals(categoryId)) {
            throw new ConflictException("A category cannot be its own parent");
        }
        Category parent = findById(parentId);
        Category ancestor = parent;
        while (ancestor != null) {
            if (ancestor.getId().equals(categoryId)) {
                throw new ConflictException("Category hierarchy cannot contain a cycle");
            }
            ancestor = ancestor.getParent();
        }
        return parent;
    }
}
