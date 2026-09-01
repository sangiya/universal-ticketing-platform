package com.ticketmesh.controller;

import com.ticketmesh.exception.NotFoundException;
import com.ticketmesh.model.ProductOrder;
import com.ticketmesh.security.TenantGuard;
import com.ticketmesh.service.ProductOrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Universal marketplace checkout. Customers order any provider product in any
 * tenant through the single order engine (validates inventory, prices, applies
 * promotions, awards loyalty, emits events).
 */
@RestController
@RequestMapping("/api/orders")
public class ProductOrderController {

    private final ProductOrderService orderService;
    private final TenantGuard tenantGuard;

    public ProductOrderController(ProductOrderService orderService,
                                  TenantGuard tenantGuard) {
        this.orderService = orderService;
        this.tenantGuard = tenantGuard;
    }

    record CreateOrderRequest(
            Long tenantId,
            @NotNull Long productId,
            @Min(1) int quantity,
            String promoCode) {
    }

    @PostMapping
    public ResponseEntity<ProductOrder> create(@Valid @RequestBody CreateOrderRequest request) {
        Long effectiveTenantId = tenantGuard.requireAccessTo(request.tenantId());
        ProductOrder order = orderService.create(
                effectiveTenantId, request.productId(), request.quantity(), request.promoCode());
        return ResponseEntity.ok(order);
    }

    @PostMapping("/checkout")
    public ResponseEntity<ProductOrder> checkout(@Valid @RequestBody CreateOrderRequest request) {
        Long effectiveTenantId = tenantGuard.requireAccessTo(request.tenantId());
        ProductOrder order = orderService.checkout(
                effectiveTenantId, request.productId(), request.quantity(), request.promoCode());
        return ResponseEntity.ok(order);
    }

    @PostMapping("/{orderRef}/pay")
    public ResponseEntity<ProductOrder> pay(@PathVariable("orderRef") String orderRef) {
        return ResponseEntity.ok(orderService.pay(orderRef));
    }

    @GetMapping("/{orderRef}")
    public ResponseEntity<ProductOrder> mine(@PathVariable("orderRef") String orderRef) {
        return orderService.findMine(orderRef)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new NotFoundException("Order not found: " + orderRef));
    }

    @GetMapping("/mine")
    public ResponseEntity<List<ProductOrder>> mine() {
        return ResponseEntity.ok(orderService.mine());
    }

    @GetMapping
    public ResponseEntity<List<ProductOrder>> byTenant(
            @RequestParam(value = "tenantId", required = false) Long tenantId) {
        Long effectiveTenantId = tenantGuard.requireAccessTo(tenantId);
        return ResponseEntity.ok(orderService.byTenant(effectiveTenantId));
    }
}
