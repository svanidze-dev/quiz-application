package com.example.quiz.controller;

import com.example.quiz.entity.Category;
import com.example.quiz.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @GetMapping
    public List<Category> getAll() {
        return categoryService.getAllCategories();
    }

    @GetMapping("/{id}")
    public Category getById(@PathVariable Long id) {
        return categoryService.getCategoryById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Category create(@RequestBody Category category) {
        return categoryService.createCategory(category);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Long id, @RequestBody Category category) {
        return categoryService.updateCategory(id, category);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categoryService.deleteCategory(id);
    }
}