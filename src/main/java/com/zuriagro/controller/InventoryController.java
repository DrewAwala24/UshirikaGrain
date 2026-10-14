package com.zuriagro.controller;

import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.inventory.InventoryBalanceResponse;
import com.zuriagro.dto.inventory.InventoryMovementResponse;
import com.zuriagro.dto.inventory.StockAdjustmentRequest;
import com.zuriagro.security.SecurityUtils;
import com.zuriagro.service.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/inventory")
@Tag(name = "Inventory & Stock Movements", description = "Endpoints for inventory tracking, balances, and audit ledger")
@SecurityRequirement(name = "bearerAuth")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/depots/{depotId}")
    @Operation(summary = "Get all inventory balances for a given depot")
    public ResponseEntity<List<InventoryBalanceResponse>> getDepotBalances(@PathVariable Long depotId) {
        List<InventoryBalanceResponse> balances = inventoryService.getBalancesByDepot(depotId);
        return ResponseEntity.ok(balances);
    }

    @GetMapping("/depots/{depotId}/grain-types/{grainTypeId}")
    @Operation(summary = "Get specific inventory balance for a depot and grain type")
    public ResponseEntity<InventoryBalanceResponse> getBalance(
            @PathVariable Long depotId,
            @PathVariable Long grainTypeId
    ) {
        InventoryBalanceResponse balance = inventoryService.getBalance(depotId, grainTypeId);
        return ResponseEntity.ok(balance);
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Retrieve immutable stock movement ledger records with pagination")
    public ResponseEntity<PagedResponse<InventoryMovementResponse>> getMovements(
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) Long grainTypeId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<InventoryMovementResponse> response = inventoryService.getMovements(depotId, grainTypeId, pageable);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/adjustments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Perform a privileged stock adjustment with mandatory reason (Admin / Depot Manager)")
    public ResponseEntity<InventoryMovementResponse> adjustStock(@Valid @RequestBody StockAdjustmentRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        InventoryMovementResponse response = inventoryService.adjustStock(request, username);
        return ResponseEntity.ok(response);
    }
}
