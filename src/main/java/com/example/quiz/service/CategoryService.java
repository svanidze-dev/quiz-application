package com.example.quiz.service;

import com.example.quiz.entity.Category;
import com.example.quiz.exception.BusinessException;
import com.example.quiz.exception.ConflictException;
import com.example.quiz.exception.ResourceNotFoundException;
import com.example.quiz.repository.CategoryRepository;
import com.example.quiz.repository.QuizRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final QuizRepository quizRepository;

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Category getCategoryById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория", id));
    }

    @Transactional
    public Category createCategory(Category data) {
        String name = requireName(data);
        if (categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Категория '" + name + "' уже существует");
        }

        Category category = new Category();
        category.setName(name);
        category.setDescription(data.getDescription());
        return categoryRepository.save(category);
    }

    @Transactional
    public Category updateCategory(Long id, Category data) {
        Category category = getCategoryById(id);
        String name = requireName(data);

        boolean nameChanged = !name.equalsIgnoreCase(category.getName());
        if (nameChanged && categoryRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Категория '" + name + "' уже существует");
        }

        category.setName(name);
        category.setDescription(data.getDescription());
        return category;
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = getCategoryById(id);
        if (quizRepository.existsByCategoryId(id)) {
            throw new ConflictException("Нельзя удалить категорию: в ней есть викторины");
        }
        categoryRepository.delete(category);
    }

    private String requireName(Category data) {
        if (data.getName() == null || data.getName().isBlank()) {
            throw new BusinessException("Название категории обязательно");
        }
        return data.getName().trim();
    }
}