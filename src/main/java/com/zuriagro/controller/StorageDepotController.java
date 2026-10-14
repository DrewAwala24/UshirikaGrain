package com.zuriagro.controller;

import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.depot.CreateStorageDepotRequest;
import com.zuriagro.dto.depot.StorageDepotResponse;
import com.zuriagro.dto.inventory.InventoryBalanceResponse;
import com.zuriagro.service.InventoryService;
import com.zuriagro.service.StorageDepotService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/depots")
@Tag(name = "Storage Depots", description = "Endpoints for managing grain storage depots and capacity")
@SecurityRequirement(name = "bearerAuth")
public class StorageDepotController {

    private final StorageDepotService storageDepotService;
    private final InventoryService inventoryService;

    public StorageDepotController(StorageDepotService storageDepotService, InventoryService inventoryService) {
        this.storageDepotService = storageDepotService;
        this.inventoryService = inventoryService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a new storage depot (Admin only)")
    public ResponseEntity<StorageDepotResponse> createDepot(@Valid @RequestBody CreateStorageDepotRequest request) {
        StorageDepotResponse response = storageDepotService.createDepot(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve depot information by ID")
    public ResponseEntity<StorageDepotResponse> getDepotById(@PathVariable Long id) {
        StorageDepotResponse response = storageDepotService.getDepotById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all active storage depots")
    public ResponseEntity<List<StorageDepotResponse>> listActiveDepots() {
        List<StorageDepotResponse> response = storageDepotService.getAllActiveDepots();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/paged")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "List storage depots with pagination")
    public ResponseEntity<PagedResponse<StorageDepotResponse>> listPagedDepots(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PagedResponse<StorageDepotResponse> response = storageDepotService.getAllDepots(pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/inventory")
    @Operation(summary = "Get current inventory balances for a storage depot")
    public ResponseEntity<List<InventoryBalanceResponse>> getDepotInventory(@PathVariable Long id) {
        List<InventoryBalanceResponse> response = inventoryService.getBalancesByDepot(id);
        return ResponseEntity.ok(response);
    }
}
