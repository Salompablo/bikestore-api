package com.bikestore.api.controller;

import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.dto.response.ProductResponse;
import com.bikestore.api.service.ProductService;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController – sort/pagination")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController controller;

    private static final Page<ProductResponse> EMPTY_PAGE = new PageImpl<>(List.of());

    // ── getCatalog ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/products (getCatalog)")
    class GetCatalog {

        @Test
        @DisplayName("returns 200 OK and delegates to service")
        void returns200_delegatesToService() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ResponseEntity<PageResponse<ProductResponse>> response =
                    controller.getCatalog(null, null, null, null, null, 0, 12, null, null);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(productService).getActiveProducts(isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
        }

        @Test
        @DisplayName("valid sortField 'price' is applied")
        void validSortField_isApplied() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getCatalog(null, null, null, null, null, 0, 12, "price", "asc");
            verify(productService).getActiveProducts(any(), any(), any(), any(), any(), captor.capture());

            Pageable pageable = captor.getValue();
            assertEquals("price", pageable.getSort().iterator().next().getProperty());
            assertEquals(Sort.Direction.ASC, pageable.getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("invalid sortField falls back to default 'name'")
        void invalidSortField_fallsBackToDefault() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getCatalog(null, null, null, null, null, 0, 12, "badField", null);
            verify(productService).getActiveProducts(any(), any(), any(), any(), any(), captor.capture());

            assertEquals(ProductController.DEFAULT_SORT_FIELD,
                    captor.getValue().getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("invalid sortDirection falls back to default ASC")
        void invalidSortDirection_fallsBackToDefault() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getCatalog(null, null, null, null, null, 0, 12, null, "sideways");
            verify(productService).getActiveProducts(any(), any(), any(), any(), any(), captor.capture());

            assertEquals(ProductController.DEFAULT_SORT_DIRECTION,
                    captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("desc sortDirection is correctly applied")
        void descDirection_isApplied() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getCatalog(null, null, null, null, null, 0, 12, "price", "desc");
            verify(productService).getActiveProducts(any(), any(), any(), any(), any(), captor.capture());

            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("page and size params are forwarded correctly")
        void pageAndSize_areForwarded() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getCatalog(null, null, null, null, null, 2, 8, null, null);
            verify(productService).getActiveProducts(any(), any(), any(), any(), any(), captor.capture());

            assertEquals(2, captor.getValue().getPageNumber());
            assertEquals(8, captor.getValue().getPageSize());
        }

        @Test
        @DisplayName("global sort is consistent — page 0 and page 1 use the same sort")
        void globalSort_isConsistentAcrossPages() {
            when(productService.getActiveProducts(any(), any(), any(), any(), any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);

            controller.getCatalog(null, null, null, null, null, 0, 10, "price", "desc");
            controller.getCatalog(null, null, null, null, null, 1, 10, "price", "desc");

            verify(productService, times(2)).getActiveProducts(any(), any(), any(), any(), any(), captor.capture());

            List<Pageable> captured = captor.getAllValues();
            assertEquals(
                    captured.get(0).getSort().iterator().next().getProperty(),
                    captured.get(1).getSort().iterator().next().getProperty()
            );
            assertEquals(
                    captured.get(0).getSort().iterator().next().getDirection(),
                    captured.get(1).getSort().iterator().next().getDirection()
            );
        }
    }

    // ── getAllProducts (admin) ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/products/admin (getAllProducts)")
    class GetAllProducts {

        @Test
        @DisplayName("valid sortField 'averageRating' is applied")
        void validSortField_averageRating_isApplied() {
            when(productService.getAllProducts(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllProducts(0, 12, "averageRating", "desc");
            verify(productService).getAllProducts(captor.capture());

            assertEquals("averageRating", captor.getValue().getSort().iterator().next().getProperty());
            assertEquals(Sort.Direction.DESC, captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("unknown sortField falls back to default 'name'")
        void unknownSortField_fallsBackToDefault() {
            when(productService.getAllProducts(any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getAllProducts(0, 12, "unknown", null);
            verify(productService).getAllProducts(captor.capture());

            assertEquals(ProductController.DEFAULT_SORT_FIELD,
                    captor.getValue().getSort().iterator().next().getProperty());
        }
    }
}
