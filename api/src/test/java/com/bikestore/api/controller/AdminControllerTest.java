package com.bikestore.api.controller;

import com.bikestore.api.dto.response.AdminOrderDetailResponse;
import com.bikestore.api.exception.ResourceNotFoundException;
import com.bikestore.api.mapper.ShippingZoneMapper;
import com.bikestore.api.repository.ShippingZoneRepository;
import com.bikestore.api.service.CheckoutFacade;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminController")
class AdminControllerTest {

    @Mock
    private OrderService orderService;
    @Mock
    private UserService userService;
    @Mock
    private ShippingZoneRepository shippingZoneRepository;
    @Mock
    private ShippingZoneMapper shippingZoneMapper;
    @Mock
    private CheckoutFacade checkoutFacade;

    @InjectMocks
    private AdminController controller;

    @Nested
    @DisplayName("GET /api/v1/admin/orders/{id}")
    class GetOrderById {

        @Test
        @DisplayName("returns 200 OK with detailed order payload")
        void returns200WithDetailedOrder() {
            AdminOrderDetailResponse detail = new AdminOrderDetailResponse(
                    77L,
                    "QUOTE_REQUESTED",
                    "SHIPPING",
                    "Ada Lovelace",
                    "ada@test.com",
                    BigDecimal.valueOf(1500000),
                    BigDecimal.ZERO,
                    BigDecimal.valueOf(1500000),
                    LocalDateTime.now().minusHours(1),
                    LocalDateTime.now(),
                    List.of(),
                    "Calle Falsa 123",
                    "7600",
                    null,
                    "+5492235551234",
                    "PENDING",
                    null,
                    null,
                    true,
                    false
            );

            when(orderService.getOrderByIdForAdmin(77L)).thenReturn(detail);

            ResponseEntity<AdminOrderDetailResponse> response = controller.getOrderById(77L);

            assertEquals(HttpStatus.OK, response.getStatusCode());
            assertEquals(detail, response.getBody());
            verify(orderService).getOrderByIdForAdmin(77L);
        }

        @Test
        @DisplayName("propagates ResourceNotFoundException when order is missing")
        void propagatesNotFound() {
            when(orderService.getOrderByIdForAdmin(99L))
                    .thenThrow(new ResourceNotFoundException("Order not found with id: 99"));

            assertThrows(ResourceNotFoundException.class, () -> controller.getOrderById(99L));
        }
    }
}
