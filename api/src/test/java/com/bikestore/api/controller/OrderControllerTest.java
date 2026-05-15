package com.bikestore.api.controller;

import com.bikestore.api.dto.response.OrderResponse;
import com.bikestore.api.dto.response.PageResponse;
import com.bikestore.api.entity.User;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.service.OrderService;
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
@DisplayName("OrderController – sort/pagination")
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    private static final Page<OrderResponse> EMPTY_PAGE = new PageImpl<>(List.of());
    private static final User DUMMY_USER = User.builder().build();
    private static final OrderResponse DUMMY_ORDER = new OrderResponse(
            1L, "PAID", "SHIPPING", null, null, List.of(), null, null, null, null, null);

    // ── getMyOrders ───────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/orders/my-orders (getMyOrders)")
    class GetMyOrders {

        @Test
        @DisplayName("returns 200 OK and delegates to service")
        void returns200_delegatesToService() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ResponseEntity<PageResponse<OrderResponse>> response =
                    controller.getMyOrders(0, 10, null, null, DUMMY_USER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            verify(orderService).getMyOrders(eq(DUMMY_USER), any(Pageable.class));
        }

        @Test
        @DisplayName("valid sortField 'totalAmount' is applied")
        void validSortField_totalAmount_isApplied() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getMyOrders(0, 10, "totalAmount", "asc", DUMMY_USER);
            verify(orderService).getMyOrders(any(), captor.capture());

            assertEquals("totalAmount", captor.getValue().getSort().iterator().next().getProperty());
            assertEquals(Sort.Direction.ASC, captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("invalid sortField falls back to default 'createdAt'")
        void invalidSortField_fallsBackToDefault() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getMyOrders(0, 10, "badField", null, DUMMY_USER);
            verify(orderService).getMyOrders(any(), captor.capture());

            assertEquals(OrderController.DEFAULT_SORT_FIELD,
                    captor.getValue().getSort().iterator().next().getProperty());
        }

        @Test
        @DisplayName("invalid sortDirection falls back to default DESC")
        void invalidSortDirection_fallsBackToDefault() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getMyOrders(0, 10, null, "sideways", DUMMY_USER);
            verify(orderService).getMyOrders(any(), captor.capture());

            assertEquals(OrderController.DEFAULT_SORT_DIRECTION,
                    captor.getValue().getSort().iterator().next().getDirection());
        }

        @Test
        @DisplayName("null sortField and sortDirection use defaults (createdAt DESC)")
        void nullParams_useDefaults() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getMyOrders(0, 10, null, null, DUMMY_USER);
            verify(orderService).getMyOrders(any(), captor.capture());

            Sort.Order order = captor.getValue().getSort().iterator().next();
            assertEquals("createdAt", order.getProperty());
            assertEquals(Sort.Direction.DESC, order.getDirection());
        }

        @Test
        @DisplayName("page and size params are forwarded correctly")
        void pageAndSize_areForwarded() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getMyOrders(3, 5, null, null, DUMMY_USER);
            verify(orderService).getMyOrders(any(), captor.capture());

            assertEquals(3, captor.getValue().getPageNumber());
            assertEquals(5, captor.getValue().getPageSize());
        }

        @Test
        @DisplayName("global sort consistency across pages")
        void globalSort_isConsistentAcrossPages() {
            when(orderService.getMyOrders(any(), any())).thenReturn(EMPTY_PAGE);

            ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
            controller.getMyOrders(0, 5, "status", "asc", DUMMY_USER);
            controller.getMyOrders(1, 5, "status", "asc", DUMMY_USER);
            verify(orderService, times(2)).getMyOrders(any(), captor.capture());

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

    // ── getMyOrderById ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /api/v1/orders/my-orders/{id} (getMyOrderById)")
    class GetMyOrderById {

        @Test
        @DisplayName("returns 200 OK with the order when it belongs to the user")
        void returns200_whenOrderBelongsToUser() {
            when(orderService.getMyOrderById(1L, DUMMY_USER)).thenReturn(DUMMY_ORDER);

            ResponseEntity<OrderResponse> response = controller.getMyOrderById(1L, DUMMY_USER);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(DUMMY_ORDER, response.getBody());
            verify(orderService).getMyOrderById(1L, DUMMY_USER);
        }

        @Test
        @DisplayName("propagates ResourceNotFoundException when order not found or not owned")
        void propagatesNotFound_whenOrderNotFoundOrNotOwned() {
            when(orderService.getMyOrderById(99L, DUMMY_USER))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            assertThrows(ResourceNotFoundException.class,
                    () -> controller.getMyOrderById(99L, DUMMY_USER));
        }
    }
}
