package com.zuriagro.controller;

import com.zuriagro.dto.graintype.CreateGrainTypeRequest;
import com.zuriagro.dto.graintype.GrainTypeResponse;
import com.zuriagro.service.GrainTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/grain-types")
@Tag(name = "Grain Types", description = "Endpoints for grain catalog and units of measure")
@SecurityRequirement(name = "bearerAuth")
public class GrainTypeController {

    private final GrainTypeService grainTypeService;

    public GrainTypeController(GrainTypeService grainTypeService) {
        this.grainTypeService = grainTypeService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Register a new grain type in catalog (Admin only)")
    public ResponseEntity<GrainTypeResponse> createGrainType(@Valid @RequestBody CreateGrainTypeRequest request) {
        GrainTypeResponse response = grainTypeService.createGrainType(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve grain type by ID")
    public ResponseEntity<GrainTypeResponse> getGrainTypeById(@PathVariable Long id) {
        GrainTypeResponse response = grainTypeService.getGrainTypeById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    @Operation(summary = "List all active grain types")
    public ResponseEntity<List<GrainTypeResponse>> listActiveGrainTypes() {
        List<GrainTypeResponse> response = grainTypeService.getAllActiveGrainTypes();
        return ResponseEntity.ok(response);
    }
}
