package com.example.dati_backend.controller;

import com.example.dati_backend.common.ApiResponse;
import com.example.dati_backend.dto.CategoryRequest;
import com.example.dati_backend.dto.CategoryTreeNode;
import com.example.dati_backend.entity.QuestionCategory;
import com.example.dati_backend.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public ApiResponse<List<QuestionCategory>> listCategories(@RequestParam(required = false) Long parentId) {
        return ApiResponse.ok(categoryService.listCategories(parentId));
    }

    @GetMapping("/tree")
    public ApiResponse<List<CategoryTreeNode>> categoryTree() {
        return ApiResponse.ok(categoryService.categoryTree());
    }

    @GetMapping("/{id}")
    public ApiResponse<QuestionCategory> getCategory(@PathVariable Long id) {
        return ApiResponse.ok(categoryService.getCategory(id));
    }

    @PostMapping
    public ApiResponse<QuestionCategory> createCategory(@RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<QuestionCategory> updateCategory(@PathVariable Long id, @RequestBody CategoryRequest request) {
        return ApiResponse.ok(categoryService.updateCategory(id, request));
    }

    @PatchMapping("/{id}/disable")
    public ApiResponse<Void> disableCategory(@PathVariable Long id) {
        categoryService.disableCategory(id);
        return ApiResponse.ok();
    }
}
