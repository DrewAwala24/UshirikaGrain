package com.zuriagro.controller;

import com.zuriagro.domain.entity.DepositStatus;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.deposit.CreateGrainDepositRequest;
import com.zuriagro.dto.deposit.DepositDecisionRequest;
import com.zuriagro.dto.deposit.GrainDepositResponse;
import com.zuriagro.security.SecurityUtils;
import com.zuriagro.service.GrainDepositService;
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
@RequestMapping("/api/v1/deposits")
@Tag(name = "Grain Deposits", description = "Endpoints for recording, retrieving, and processing grain deposits")
@SecurityRequirement(name = "bearerAuth")
public class GrainDepositController {

    private final GrainDepositService grainDepositService;

    public GrainDepositController(GrainDepositService grainDepositService) {
        this.grainDepositService = grainDepositService;
    }

    @PostMapping
    @Operation(summary = "Submit a grain deposit record")
    public ResponseEntity<GrainDepositResponse> createDeposit(@Valid @RequestBody CreateGrainDepositRequest request) {
        String username = SecurityUtils.getCurrentUsername();
        GrainDepositResponse response = grainDepositService.createDeposit(request, username);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve deposit details by ID")
    public ResponseEntity<GrainDepositResponse> getDepositById(@PathVariable Long id) {
        String username = SecurityUtils.getCurrentUsername();
        GrainDepositResponse response = grainDepositService.getDepositById(id, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Retrieve deposit details by reference number")
    public ResponseEntity<GrainDepositResponse> getDepositByReference(@PathVariable String reference) {
        String username = SecurityUtils.getCurrentUsername();
        GrainDepositResponse response = grainDepositService.getDepositByReference(reference, username);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List grain deposits with filtering and pagination")
    public ResponseEntity<PagedResponse<GrainDepositResponse>> listDeposits(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) Long depotId,
            @RequestParam(required = false) DepositStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        String username = SecurityUtils.getCurrentUsername();
        PagedResponse<GrainDepositResponse> response = grainDepositService.getDeposits(
                farmerId, depotId, status, pageable, username
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/accept")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Accept a grain deposit and atomically credit depot inventory")
    public ResponseEntity<GrainDepositResponse> acceptDeposit(
            @PathVariable Long id,
            @RequestBody(required = false) DepositDecisionRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        GrainDepositResponse response = grainDepositService.acceptDeposit(id, request, username);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN', 'DEPOT_MANAGER')")
    @Operation(summary = "Reject a grain deposit with a mandatory reason")
    public ResponseEntity<GrainDepositResponse> rejectDeposit(
            @PathVariable Long id,
            @Valid @RequestBody DepositDecisionRequest request
    ) {
        String username = SecurityUtils.getCurrentUsername();
        GrainDepositResponse response = grainDepositService.rejectDeposit(id, request, username);
        return ResponseEntity.ok(response);
    }
}
