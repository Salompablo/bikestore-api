package com.bikestore.api.service;

import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.CheckoutInfo;
import com.bikestore.api.dto.response.CheckoutResponse;
import com.bikestore.api.entity.Category;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.Role;
import com.bikestore.api.event.ShippingQuotePublishedData;
import com.bikestore.api.event.ShippingQuoteRequestedData;
import com.bikestore.api.repository.CategoryRepository;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
import com.bikestore.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest
class ShippingQuotePublishEmailFlowIntegrationTest {

    @Autowired
    private CheckoutFacade checkoutFacade;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private OrderRepository orderRepository;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    @MockitoBean
    private EmailService emailService;

    private User testUser;
    private Product checkoutProduct;
    private Long createdOrderId;

    @BeforeEach
    void setUp() {
        stockReservationRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        Category category = categoryRepository.save(Category.builder()
                .name("Shipping Email Category " + UUID.randomUUID())
                .build());

        checkoutProduct = productRepository.save(Product.builder()
                .sku("SKU-" + UUID.randomUUID())
                .name("Email Trigger Bike")
                .price(BigDecimal.valueOf(1000))
                .stock(10)
                .reservedStock(0)
                .category(category)
                .build());

        testUser = userRepository.save(User.builder()
                .email("shipping-email-" + UUID.randomUUID() + "@test.com")
                .password("$2a$10$hashedPassword")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(true)
                .build());

        CheckoutRequest shippingCheckout = new CheckoutRequest(
                List.of(new CartItemRequest(checkoutProduct.getId(), 1)),
                DeliveryMethod.SHIPPING,
                "Calle Test 123",
                "7600",
                null,
                "+5492235550000",
                false
        );

        CheckoutResponse response = checkoutFacade.initializeCheckout(shippingCheckout, testUser);
        createdOrderId = response.orderId();
        when(paymentGatewayService.createPreference(any()))
                .thenReturn(new CheckoutInfo("pref-test-" + response.orderId(), "https://mp.test/init"));
    }

    @Test
    @DisplayName("Requesting shipping quote triggers admin email flow after commit")
    void requestShippingQuoteTriggersAdminEmailFlow() {
        verify(emailService, timeout(3000)).sendAdminShippingQuoteRequest(any(), argThat(data ->
                hasExpectedAdminEmailData(data, createdOrderId, testUser.getEmail())
        ));
    }

    @Test
    @DisplayName("Publishing shipping quote triggers customer email flow")
    void publishShippingQuoteTriggersCustomerEmailFlow() {
        Long orderId = orderRepository.findAll().getFirst().getId();

        CheckoutResponse response = checkoutFacade.publishShippingQuote(orderId, BigDecimal.valueOf(1500));

        assertEquals(orderId, response.orderId());
        assertFalse(response.requiresShippingQuote());

        verify(emailService, timeout(3000)).sendCustomerShippingQuoteReady(argThat(data ->
                hasExpectedCustomerEmailData(data, orderId, testUser.getEmail())
        ));
    }

    private boolean hasExpectedCustomerEmailData(
            ShippingQuotePublishedData data,
            Long expectedOrderId,
            String expectedEmail
    ) {
        return data != null
                && expectedOrderId.equals(data.orderId())
                && expectedEmail.equals(data.customerEmail())
                && BigDecimal.valueOf(1000).compareTo(data.productsSubtotal()) == 0
                && data.items() != null
                && data.items().size() == 1
                && "Email Trigger Bike".equals(data.items().getFirst().productName())
                && data.items().getFirst().quantity() == 1
                && BigDecimal.valueOf(1000).compareTo(data.items().getFirst().lineTotal()) == 0;
    }

    private boolean hasExpectedAdminEmailData(
            ShippingQuoteRequestedData data,
            Long expectedOrderId,
            String expectedEmail
    ) {
        return data != null
                && expectedOrderId.equals(data.orderId())
                && expectedEmail.equals(data.customerEmail())
                && BigDecimal.valueOf(1000).compareTo(data.productsSubtotal()) == 0
                && data.items() != null
                && data.items().size() == 1
                && "Email Trigger Bike".equals(data.items().getFirst().productName())
                && data.items().getFirst().quantity() == 1
                && BigDecimal.valueOf(1000).compareTo(data.items().getFirst().lineTotal()) == 0;
    }
}
