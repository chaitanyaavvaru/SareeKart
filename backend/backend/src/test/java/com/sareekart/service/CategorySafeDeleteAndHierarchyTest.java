package com.sareekart.service;

import com.sareekart.dto.request.CategoryRequest;
import com.sareekart.dto.response.CategoryResponse;
import com.sareekart.entity.Category;
import com.sareekart.exception.BadRequestException;
import com.sareekart.mapper.CategoryMapper;
import com.sareekart.repository.CategoryRepository;
import com.sareekart.repository.ProductRepository;
import com.sareekart.service.impl.CategoryServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategorySafeDeleteAndHierarchyTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category parentCategory;
    private Category childCategory;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .id(1L)
                .name("Silk Sarees")
                .slug("silk-sarees")
                .active(true)
                .displayOrder(1)
                .build();

        childCategory = Category.builder()
                .id(2L)
                .name("Kanchipuram Silk")
                .slug("kanchipuram-silk")
                .parent(parentCategory)
                .active(true)
                .displayOrder(1)
                .build();
    }

    @Test
    @DisplayName("Safeguard 4: Deletion REJECTED when products are assigned to category")
    void deleteCategory_WhenProductsExist_ThrowsBadRequest() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(productRepository.countByCategoryId(1L)).thenReturn(14L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            categoryService.deleteCategory(1L);
        });

        assertTrue(ex.getMessage().contains("Cannot delete category 'Silk Sarees' because it is associated with 14 product(s)"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Safeguard 4: Deletion REJECTED when subcategories exist")
    void deleteCategory_WhenSubcategoriesExist_ThrowsBadRequest() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(productRepository.countByCategoryId(1L)).thenReturn(0L);
        when(categoryRepository.countByParentId(1L)).thenReturn(3L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> {
            categoryService.deleteCategory(1L);
        });

        assertTrue(ex.getMessage().contains("because it has 3 subcategories"));
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    @DisplayName("Safeguard 4: Deletion SUCCEEDS when 0 products and 0 subcategories exist")
    void deleteCategory_WhenClean_Succeeds() {
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(childCategory));
        when(productRepository.countByCategoryId(2L)).thenReturn(0L);
        when(categoryRepository.countByParentId(2L)).thenReturn(0L);

        assertDoesNotThrow(() -> categoryService.deleteCategory(2L));
        verify(categoryRepository, times(1)).delete(childCategory);
    }

    @Test
    @DisplayName("Soft Deactivation: Toggles category status successfully")
    void toggleCategoryStatus_TogglesActiveState() {
        when(categoryRepository.findById(1L)).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenAnswer(i -> i.getArgument(0));
        when(categoryMapper.toResponse(any(Category.class))).thenAnswer(i -> {
            Category c = i.getArgument(0);
            return CategoryResponse.builder().id(c.getId()).active(c.getActive()).build();
        });

        CategoryResponse res = categoryService.toggleCategoryStatus(1L);
        assertFalse(parentCategory.getActive());
        verify(categoryRepository, times(1)).save(parentCategory);
    }

    @Test
    @DisplayName("Category Hierarchy: Tree retrieval maps root categories with children")
    void getCategoryTree_ReturnsRootWithSubcategories() {
        when(categoryRepository.findByParentIsNullAndActiveTrueOrderByDisplayOrderAsc())
                .thenReturn(List.of(parentCategory));
        when(categoryMapper.toResponseTree(parentCategory))
                .thenReturn(CategoryResponse.builder()
                        .id(1L)
                        .name("Silk Sarees")
                        .subcategories(List.of(CategoryResponse.builder().id(2L).name("Kanchipuram Silk").build()))
                        .build());

        List<CategoryResponse> tree = categoryService.getCategoryTree();
        assertEquals(1, tree.size());
        assertEquals("Silk Sarees", tree.get(0).getName());
        assertEquals(1, tree.get(0).getSubcategories().size());
        assertEquals("Kanchipuram Silk", tree.get(0).getSubcategories().get(0).getName());
    }
}
