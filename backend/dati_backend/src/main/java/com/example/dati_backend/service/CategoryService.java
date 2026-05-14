package com.example.dati_backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.example.dati_backend.dto.CategoryRequest;
import com.example.dati_backend.dto.CategoryTreeNode;
import com.example.dati_backend.entity.QuestionCategory;
import com.example.dati_backend.mapper.QuestionMapper;
import com.example.dati_backend.mapper.QuestionCategoryMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CategoryService {
    private static final Duration CATEGORY_CACHE_TTL = Duration.ofMinutes(10);
    private static final String CATEGORY_TREE_CACHE_KEY = "dati:category:tree";
    private static final String CATEGORY_LIST_CACHE_PREFIX = "dati:category:list:";

    private final QuestionCategoryMapper categoryMapper;
    private final QuestionMapper questionMapper;
    private final RedisJsonCacheService cacheService;

    public List<QuestionCategory> listCategories(Long parentId) {
        String cacheKey = categoryListCacheKey(parentId);
        TypeReference<List<QuestionCategory>> typeReference = new TypeReference<>() {};
        return cacheService.get(cacheKey, typeReference)
                .orElseGet(() -> {
                    List<QuestionCategory> categories = loadCategories(parentId);
                    cacheService.set(cacheKey, categories, CATEGORY_CACHE_TTL);
                    return categories;
                });
    }

    private List<QuestionCategory> loadCategories(Long parentId) {
        if (parentId == null || parentId == 0) {
            return categoryMapper.listAll();
        }
        return categoryMapper.listByParentId(parentId);
    }

    public List<CategoryTreeNode> categoryTree() {
        TypeReference<List<CategoryTreeNode>> typeReference = new TypeReference<>() {};
        return cacheService.get(CATEGORY_TREE_CACHE_KEY, typeReference)
                .orElseGet(() -> {
                    List<CategoryTreeNode> tree = buildCategoryTree();
                    cacheService.set(CATEGORY_TREE_CACHE_KEY, tree, CATEGORY_CACHE_TTL);
                    return tree;
                });
    }

    private List<CategoryTreeNode> buildCategoryTree() {
        List<QuestionCategory> categories = categoryMapper.listAll();
        Map<Long, CategoryTreeNode> nodeMap = new LinkedHashMap<>();
        List<CategoryTreeNode> roots = new ArrayList<>();

        for (QuestionCategory category : categories) {
            nodeMap.put(category.getId(), toTreeNode(category));
        }
        for (QuestionCategory category : categories) {
            CategoryTreeNode node = nodeMap.get(category.getId());
            if (category.getParentId() == null || !nodeMap.containsKey(category.getParentId())) {
                roots.add(node);
            } else {
                nodeMap.get(category.getParentId()).getChildren().add(node);
            }
        }
        return roots;
    }

    public QuestionCategory getCategory(Long id) {
        QuestionCategory category = categoryMapper.findById(id);
        if (category == null) {
            throw new IllegalArgumentException("Category not found");
        }
        return category;
    }

    @Transactional
    public QuestionCategory createCategory(CategoryRequest request) {
        QuestionCategory category = new QuestionCategory();
        fillCategory(category, request, true);
        categoryMapper.insert(category);
        clearCategoryCache();
        return getCategory(category.getId());
    }

    @Transactional
    public QuestionCategory updateCategory(Long id, CategoryRequest request) {
        QuestionCategory category = getCategory(id);
        fillCategory(category, request, false);
        categoryMapper.update(category);
        clearCategoryCache();
        return getCategory(id);
    }

    @Transactional
    public void disableCategory(Long id) {
        getCategory(id);
        categoryMapper.updateStatus(id, "DISABLED");
        clearCategoryCache();
    }

    @Transactional
    public void deleteCategory(Long id) {
        getCategory(id);
        if (categoryMapper.countChildren(id) > 0) {
            throw new IllegalArgumentException("该分类下还有子分类，不能删除");
        }
        if (questionMapper.countByCategoryId(id) > 0) {
            throw new IllegalArgumentException("该分类下还有题目，不能删除");
        }
        categoryMapper.deleteById(id);
        clearCategoryCache();
    }

    private void clearCategoryCache() {
        cacheService.delete(CATEGORY_TREE_CACHE_KEY);
        cacheService.deleteByPattern(CATEGORY_LIST_CACHE_PREFIX + "*");
    }

    private String categoryListCacheKey(Long parentId) {
        return CATEGORY_LIST_CACHE_PREFIX + (parentId == null || parentId == 0 ? "all" : "parent:" + parentId);
    }

    private void fillCategory(QuestionCategory category, CategoryRequest request, boolean creating) {
        if (request == null) {
            throw new IllegalArgumentException("Category request is required");
        }
        if (creating && !StringUtils.hasText(request.name())) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (StringUtils.hasText(request.name())) {
            category.setName(request.name().trim());
        }

        Long parentId = normalizeParentId(request.parentId());
        boolean parentChanged = creating || request.parentId() != null;
        if (parentChanged) {
            category.setParentId(parentId);
        }

        Integer level = request.level();
        if (parentChanged && parentId != null) {
            QuestionCategory parent = getCategory(parentId);
            level = parent.getLevel() + 1;
        } else if (parentChanged && level == null) {
            level = 1;
        }
        if (level != null) {
            if (level < 1 || level > 3) {
                throw new IllegalArgumentException("Category level must be between 1 and 3");
            }
            category.setLevel(level);
        }
        if (creating || request.sortNo() != null) {
            category.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        }
        if (creating || StringUtils.hasText(request.status())) {
            category.setStatus(StringUtils.hasText(request.status()) ? request.status().trim().toUpperCase() : "ENABLED");
        }
    }

    private Long normalizeParentId(Long parentId) {
        return parentId == null || parentId == 0 ? null : parentId;
    }

    private CategoryTreeNode toTreeNode(QuestionCategory category) {
        CategoryTreeNode node = new CategoryTreeNode();
        node.setId(category.getId());
        node.setParentId(category.getParentId());
        node.setName(category.getName());
        node.setLevel(category.getLevel());
        node.setSortNo(category.getSortNo());
        node.setStatus(category.getStatus());
        return node;
    }
}
