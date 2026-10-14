package com.zuriagro.controller;

import com.zuriagro.domain.entity.FarmerStatus;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.farmer.CreateFarmerRequest;
import com.zuriagro.dto.farmer.FarmerDetailResponse;
import com.zuriagro.dto.farmer.FarmerResponse;
import com.zuriagro.dto.farmer.UpdateFarmerRequest;
import com.zuriagro.security.SecurityUtils;
import com.zuriagro.service.FarmerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/farmers")
@Tag(name = "Farmers Management", description = "Endpoints for farmer registration, profile management, and deposit records")
@SecurityRequirement(name = "bearerAuth")
public class FarmerController {

    private final FarmerService farmerService;

    public FarmerController(FarmerService farmerService) {
        this.farmerService = farmerService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Register a new farmer profile")
    public ResponseEntity<FarmerResponse> registerFarmer(@Valid @RequestBody CreateFarmerRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        FarmerResponse response = farmerService.createFarmer(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "List and search farmers with pagination and status filtering")
    public ResponseEntity<PagedResponse<FarmerResponse>> getFarmers(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) FarmerStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        PagedResponse<FarmerResponse> response = farmerService.getFarmers(search, status, pageable);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER', 'FARMER')")
    @Operation(summary = "Get farmer details and recent deposits")
    public ResponseEntity<FarmerDetailResponse> getFarmerById(@PathVariable Long id) {
        FarmerDetailResponse response = farmerService.getFarmerDetail(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Look up a farmer by system farmer code")
    public ResponseEntity<FarmerResponse> getFarmerByCode(@PathVariable String code) {
        FarmerResponse response = farmerService.getFarmerByCode(code);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Update farmer details")
    public ResponseEntity<FarmerResponse> updateFarmer(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFarmerRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        FarmerResponse response = farmerService.updateFarmer(id, request, username);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Activate or deactivate a farmer profile")
    public ResponseEntity<FarmerResponse> setStatus(
            @PathVariable Long id,
            @RequestParam FarmerStatus status
    ) {
        String username = SecurityUtils.getCurrentUsername();
        FarmerResponse response = farmerService.setFarmerStatus(id, status, username);
        return ResponseEntity.ok(response);
    }
}
