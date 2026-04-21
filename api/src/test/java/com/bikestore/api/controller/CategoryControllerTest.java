package com.bikestore.api.controller;

import com.bikestore.api.dto.response.CategoryResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.service.CategoryService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController – sort/pagination")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController controller;

    private static final Page<CategoryResponse> EMPTY_PAGE = new PageImpl<>(List.of());

    // ── getAllCategories ───────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/categories (getAllCategories)")
    class GetAllCategories {

        @Test
        @DisplayName("returns 200 OK and delegates to service")
        void returns200_delegatesToService() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);

            ResponseEntity<PageResponse<CategoryResponse>> response =
                    controller.getAllCategories(0, 20, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(categoryService).getAllCategories(any(Pageable.class));
        }

        @Test
        @DisplayName("returns PageResponse (not a raw List)")
        void returnsPageResponse() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);
            ResponseEntity<PageResponse<CategoryResponse>> response =
                    controller.getAllCategories(0, 20, null, null);
            assertNotNull(response.getBody());
            assertNotNull(response.getBody().page());
        }

        @Test
        @DisplayName("valid sortField 'name' is applied")
        void validSortField_isApplied() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllCategories(0, 20, "name", "asc");
            verify(categoryService).getAllCategories(captor.capture());

            assertEquals("name", captor.getValue().getSort().iterator().next().getProperty());
            assertEquals(Sort.Direction.ASC, captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("invalid sortField falls back to default 'name'")
        void invalidSortField_fallsBackToDefault() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllCategories(0, 20, "badField", null);
            verify(categoryService).getAllCategories(captor.capture());

            assertEquals(CategoryController.DEFAULT_SORT_FIELD,
                    captor.getValue().getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("invalid sortDirection falls back to default ASC")
        void invalidSortDirection_fallsBackToDefault() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllCategories(0, 20, null, "invalid");
            verify(categoryService).getAllCategories(captor.capture());

            assertEquals(CategoryController.DEFAULT_SORT_DIRECTION,
                    captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("desc sortDirection is correctly applied")
        void descDirection_isApplied() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllCategories(0, 20, "name", "desc");
            verify(categoryService).getAllCategories(captor.capture());

            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("page and size params are forwarded correctly")
        void pageAndSize_areForwarded() {
            when(categoryService.getAllCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllCategories(1, 5, null, null);
            verify(categoryService).getAllCategories(captor.capture());

            assertEquals(1, captor.getValue().getPageNumber());
            assertEquals(5, captor.getValue().getPageSize());
        }
    }

    // ── getActiveCategories ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/categories/active (getActiveCategories)")
    class GetActiveCategories {

        @Test
        @DisplayName("returns 200 OK and delegates to service")
        void returns200_delegatesToService() {
            when(categoryService.getActiveCategories(any())).thenReturn(EMPTY_PAGE);

            ResponseEntity<PageResponse<CategoryResponse>> response =
                    controller.getActiveCategories(0, 20, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(categoryService).getActiveCategories(any(Pageable.class));
        }

        @Test
        @DisplayName("valid sortField 'id' is applied")
        void validSortField_id_isApplied() {
            when(categoryService.getActiveCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getActiveCategories(0, 20, "id", "desc");
            verify(categoryService).getActiveCategories(captor.capture());

            assertEquals("id", captor.getValue().getSort().iterator().next().getProperty());
            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("global sort consistency across pages for active categories")
        void globalSort_isConsistentAcrossPages() {
            when(categoryService.getActiveCategories(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getActiveCategories(0, 10, "name", "desc");
            controller.getActiveCategories(1, 10, "name", "desc");
            verify(categoryService, times(2)).getActiveCategories(captor.capture());

            List<Pageable> values = captor.getAllValues();
            assertEquals(
                    values.get(0).getSort().iterator().next().getProperty(),
                    values.get(1).getSort().iterator().next().getProperty()
            );
            assertEquals(
                    values.get(0).getSort().iterator().next().getDirection(),
                    values.get(1).getSort().iterator().next().getDirection()
            );
        }
    }
}
