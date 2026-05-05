package com.bikestore.api;

import com.bikestore.api.dto.request.CartItemRequest;
import com.bikestore.api.dto.request.CheckoutRequest;
import com.bikestore.api.dto.response.PaymentInfo;
import com.bikestore.api.entity.Category;
import com.bikestore.api.entity.Order;
import com.bikestore.api.entity.Product;
import com.bikestore.api.entity.StockReservation;
import com.bikestore.api.entity.User;
import com.bikestore.api.entity.WebhookEvent;
import com.bikestore.api.entity.enums.DeliveryMethod;
import com.bikestore.api.entity.enums.OrderStatus;
import com.bikestore.api.entity.enums.ReservationStatus;
import com.bikestore.api.entity.enums.Role;
import com.bikestore.api.entity.enums.WebhookEventStatus;
import com.bikestore.api.exception.ConflictException;
import com.bikestore.api.repository.CategoryRepository;
import com.bikestore.api.repository.OrderRepository;
import com.bikestore.api.repository.ProductRepository;
import com.bikestore.api.repository.StockReservationRepository;
import com.bikestore.api.repository.UserRepository;
import com.bikestore.api.repository.WebhookEventRepository;
import com.bikestore.api.service.CheckoutFacade;
import com.bikestore.api.service.OrderCleanupService;
import com.bikestore.api.service.OrderService;
import com.bikestore.api.service.PaymentGatewayService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@SpringBootTest
class StockReservationIntegrationTest {

    @Autowired private OrderService orderService;
    @Autowired private CheckoutFacade checkoutFacade;
    @Autowired private OrderCleanupService orderCleanupService;

    @Autowired private ProductRepository productRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private CategoryRepository categoryRepository;
    @Autowired private StockReservationRepository stockReservationRepository;
    @Autowired private WebhookEventRepository webhookEventRepository;
    @Autowired private OrderRepository orderRepository;

    @MockitoBean
    private PaymentGatewayService paymentGatewayService;

    private User testUser;
    private Category testCategory;

    // ── Test setup ────────────────────────────────────────────────────────────

    @BeforeEach
    void setUp() {
        // Delete in FK-safe order
        stockReservationRepository.deleteAll();
        orderRepository.deleteAll();
        webhookEventRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        categoryRepository.deleteAll();

        testCategory = categoryRepository.save(Category.builder()
                .name("Integration Category " + UUID.randomUUID())
                .build());

        testUser = userRepository.save(User.builder()
                .email("integration-" + UUID.randomUUID() + "@test.com")
                .password("$2a$10$hashedPassword")
                .role(Role.CUSTOMER)
                .isActive(true)
                .isEmailVerified(true)
                .build());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Product createProduct(int stock) {
        return productRepository.save(Product.builder()
                .sku("SKU-" + UUID.randomUUID())
                .name("Test Bike")
                .price(BigDecimal.valueOf(999.99))
                .stock(stock)
                .reservedStock(0)
                .category(testCategory)
                .build());
    }

    private CheckoutRequest checkoutFor(Long productId, int qty) {
        return new CheckoutRequest(
                List.of(new CartItemRequest(productId, qty)),
                DeliveryMethod.STORE_PICKUP,
                null, null, null,
                "+5492235550000",
                false
        );
    }

    // ── Test 1: Concurrency ───────────────────────────────────────────────────

    @Test
    @DisplayName("Concurrency: only 1 of 3 simultaneous orders succeeds when stock=1, no overselling")
    void testConcurrency_NoOverselling() throws Exception {
        Product product = createProduct(1);
        CheckoutRequest request = checkoutFor(product.getId(), 1);

        int threadCount = 3;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch go = new CountDownLatch(1);
        List<Future<Order>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                ready.countDown();
                go.await();
                return orderService.createPendingOrder(request, testUser);
            }));
        }

        ready.await();  // wait until all threads are staged
        go.countDown(); // release all simultaneously
        executor.shutdown();
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        int successCount = 0;
        int conflictCount = 0;
        for (Future<Order> f : futures) {
            try {
                f.get();
                successCount++;
            } catch (ExecutionException e) {
                if (e.getCause() instanceof ConflictException) {
                    conflictCount++;
                } else {
                    throw e;
                }
            }
        }

        assertEquals(1, successCount, "Exactly 1 order should succeed");
        assertEquals(threadCount - 1, conflictCount, "The remaining threads must fail with ConflictException");

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(1, refreshed.getReservedStock(), "reservedStock must be exactly 1 — no overselling");
    }

    // ── Test 2: TTL / Expiry ──────────────────────────────────────────────────

    @Test
    @DisplayName("TTL: expired reservation releases reservedStock, marks reservation EXPIRED and order CANCELLED")
    void testExpiry_CleansUpReservation() {
        Product product = createProduct(5);
        int physicalStock = product.getStock();

        // Create order → reservedStock should become 1
        Order order = orderService.createPendingOrder(checkoutFor(product.getId(), 1), testUser);

        Product afterReserve = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(1, afterReserve.getReservedStock(), "reservedStock should be 1 after reservation");

        // Force the reservation's expiresAt into the past
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(order.getId());
        assertFalse(reservations.isEmpty(), "There must be at least one reservation");
        StockReservation reservation = reservations.get(0);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(20));
        stockReservationRepository.save(reservation);

        // Manually trigger the cleanup scheduler
        orderCleanupService.releaseExpiredReservations();

        // ── Assertions ────────────────────────────────────────────────────────
        Product afterCleanup = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, afterCleanup.getReservedStock(),
                "reservedStock must drop back to 0 after cleanup");
        assertEquals(physicalStock, afterCleanup.getStock(),
                "physical stock must NOT change — only reservedStock is adjusted");

        StockReservation updatedRes = stockReservationRepository.findById(reservation.getId()).orElseThrow();
        assertEquals(ReservationStatus.EXPIRED, updatedRes.getStatus(),
                "reservation must be marked EXPIRED");

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.CANCELLED, updatedOrder.getStatus(),
                "order must be CANCELLED");
    }

    // ── Test 4: confirmOrder vs cancelOrder race ──────────────────────────────

    @Test
    @DisplayName("Race: confirmOrder and cancelOrder on the same order are mutually exclusive")
    void testConfirmCancelRace() throws Exception {
        Product product = createProduct(5);
        Order order = orderService.createPendingOrder(checkoutFor(product.getId(), 1), testUser);
        Long orderId = order.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Future<?> confirmFuture = executor.submit(() -> {
            ready.countDown();
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            orderService.confirmOrder(orderId);
        });

        Future<?> cancelFuture = executor.submit(() -> {
            ready.countDown();
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            orderService.cancelOrder(orderId, testUser);
        });

        ready.await();
        go.countDown();
        executor.shutdown();
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        // Absorb ConflictException: one operation wins; the other may throw
        try { confirmFuture.get(); } catch (ExecutionException e) { /* acceptable */ }
        try { cancelFuture.get(); } catch (ExecutionException e) { /* acceptable */ }

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();
        assertTrue(refreshed.getReservedStock() >= 0,
                "reservedStock must never be negative");
        assertTrue(refreshed.getReservedStock() <= refreshed.getStock(),
                "reservedStock must not exceed total stock");

        Order finalOrder = orderRepository.findById(orderId).orElseThrow();
        assertTrue(finalOrder.getStatus() == OrderStatus.PAID
                        || finalOrder.getStatus() == OrderStatus.CANCELLED,
                "Order must be in exactly one terminal state (PAID or CANCELLED)");
    }

    // ── Test 5: confirmOrder vs expireReservation race ────────────────────────

    @Test
    @DisplayName("Race: confirmOrder and expireReservation on the same reservation are mutually exclusive")
    void testConfirmExpireRace() throws Exception {
        Product product = createProduct(5);
        Order order = orderService.createPendingOrder(checkoutFor(product.getId(), 1), testUser);
        Long orderId = order.getId();

        StockReservation reservation = stockReservationRepository.findByOrderId(orderId).get(0);
        reservation.setExpiresAt(LocalDateTime.now().minusMinutes(20));
        stockReservationRepository.save(reservation);
        Long reservationId = reservation.getId();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);

        Future<?> confirmFuture = executor.submit(() -> {
            ready.countDown();
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            orderService.confirmOrder(orderId);
        });

        Future<?> expireFuture = executor.submit(() -> {
            ready.countDown();
            try { go.await(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            orderCleanupService.expireReservation(reservationId);
        });

        ready.await();
        go.countDown();
        executor.shutdown();
        executor.awaitTermination(30, java.util.concurrent.TimeUnit.SECONDS);

        // Both operations are safe-to-no-op when they lose the race
        try { confirmFuture.get(); } catch (ExecutionException e) { /* acceptable */ }
        try { expireFuture.get(); } catch (ExecutionException e) { /* acceptable */ }

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();
        assertTrue(refreshed.getReservedStock() >= 0,
                "reservedStock must never be negative");
        assertTrue(refreshed.getReservedStock() <= refreshed.getStock(),
                "reservedStock must not exceed total stock");
    }

    // ── Test 6: createPreference failure releases stock immediately ───────────

    @Test
    @DisplayName("Compensation: createPreference failure triggers immediate stock release")
    void testCreatePreferenceFailureReleasesStock() {
        Product product = createProduct(5);

        doThrow(new RuntimeException("Simulated MP failure"))
                .when(paymentGatewayService).createPreference(any());

        org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> checkoutFacade.initializeCheckout(checkoutFor(product.getId(), 1), testUser));

        Product refreshed = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, refreshed.getReservedStock(),
                "reservedStock must be 0 after MP failure compensation");

        List<Order> orders = orderRepository.findAll();
        assertFalse(orders.isEmpty(), "An order must have been created");
        Order createdOrder = orders.get(0);
        assertEquals(OrderStatus.CANCELLED, createdOrder.getStatus(),
                "Order must be CANCELLED after failed checkout");

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(createdOrder.getId());
        assertFalse(reservations.isEmpty(), "Reservation must exist");
        assertEquals(ReservationStatus.EXPIRED, reservations.get(0).getStatus(),
                "Reservation must be EXPIRED after compensation");
    }

    // ── Test 7: cancelOrder idempotent ────────────────────────────────────────

    @Test
    @DisplayName("Idempotency: double cancelOrder returns without exception and does not double-release stock")
    void testCancelOrderIdempotent() {
        Product product = createProduct(5);
        Order order = orderService.createPendingOrder(checkoutFor(product.getId(), 1), testUser);
        Long orderId = order.getId();

        // First cancel — performs the release
        orderService.cancelOrder(orderId, testUser);

        Product afterFirstCancel = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, afterFirstCancel.getReservedStock(),
                "reservedStock must be 0 after first cancel");

        // Second cancel — must be a no-op, no exception, no negative stock
        assertDoesNotThrow(() -> orderService.cancelOrder(orderId, testUser),
                "Second cancelOrder must not throw");

        Product afterSecondCancel = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(0, afterSecondCancel.getReservedStock(),
                "reservedStock must still be 0 after second cancel — no double-release");
        assertEquals(5, afterSecondCancel.getStock(),
                "Physical stock must not change on cancel");
    }

    // ── Test 8: reserved_stock invariant ─────────────────────────────────────

    @Test
    @DisplayName("Invariant: 0 <= reservedStock <= stock after mixed reserve/release/confirm operations")
    void testReservedStockInvariant() {
        Product productA = createProduct(10);
        Product productB = createProduct(3);

        // Reserve product A (qty 3) and confirm the order
        Order orderA = orderService.createPendingOrder(checkoutFor(productA.getId(), 3), testUser);
        orderService.confirmOrder(orderA.getId());

        // Reserve product B (qty 3, full stock) and cancel the order
        Order orderB = orderService.createPendingOrder(checkoutFor(productB.getId(), 3), testUser);
        orderService.cancelOrder(orderB.getId(), testUser);

        // Reserve product A again (qty 2) and let it expire
        Order orderC = orderService.createPendingOrder(checkoutFor(productA.getId(), 2), testUser);
        StockReservation res = stockReservationRepository.findByOrderId(orderC.getId()).get(0);
        res.setExpiresAt(LocalDateTime.now().minusMinutes(5));
        stockReservationRepository.save(res);
        orderCleanupService.releaseExpiredReservations();

        // Assert invariant on all products
        productRepository.findAll().forEach(p -> {
            assertTrue(p.getReservedStock() >= 0,
                    "reservedStock must be >= 0 for product " + p.getId());
            assertTrue(p.getReservedStock() <= p.getStock(),
                    "reservedStock must be <= stock for product " + p.getId());
        });
    }

    // ── Test 3: Webhook Idempotency ───────────────────────────────────────────

    @Test
    @DisplayName("Idempotency: duplicate webhook does not double-deduct stock or create extra events")
    void testWebhookIdempotency() {
        Product product = createProduct(5);
        int physicalStock = product.getStock();

        // Create an order so there is a reservation to confirm
        Order order = orderService.createPendingOrder(checkoutFor(product.getId(), 1), testUser);
        Long orderId = order.getId();

        long fakePaymentId = 99999L;
        String eventId = "EVENT-IDEMPOTENCY-" + UUID.randomUUID();

        // Mock: payment gateway returns "approved" for any paymentId
        when(paymentGatewayService.getPaymentInfo(anyLong()))
                .thenReturn(new PaymentInfo("approved", orderId.toString()));

        // First webhook call — should process normally
        assertDoesNotThrow(() -> checkoutFacade.processWebHook(fakePaymentId, eventId));

        // Second webhook call — duplicate; must be silently ignored
        assertDoesNotThrow(() -> checkoutFacade.processWebHook(fakePaymentId, eventId));

        // ── Assertions ────────────────────────────────────────────────────────
        List<WebhookEvent> events = webhookEventRepository.findAll();
        assertEquals(1, events.size(), "Only 1 webhook event must be persisted");
        assertEquals(WebhookEventStatus.PROCESSED, events.get(0).getStatus(),
                "The single event must be PROCESSED");

        // Stock deducted exactly once: stock = initialStock - 1, reservedStock = 0
        Product afterWebhook = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(physicalStock - 1, afterWebhook.getStock(),
                "Physical stock must decrease by 1 (deducted exactly once)");
        assertEquals(0, afterWebhook.getReservedStock(),
                "reservedStock must be 0 after the reservation is consumed");
    }
}