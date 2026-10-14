package com.zuriagro.dto.inventory;

import com.zuriagro.domain.entity.MovementType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record StockAdjustmentRequest(
        @NotNull(message = "Depot ID is required")
        Long depotId,

        @NotNull(message = "Grain type ID is required")
        Long grainTypeId,

        @NotNull(message = "Movement type is required (ADJUSTMENT_ADD or ADJUSTMENT_SUBTRACT)")
        MovementType movementType,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be strictly positive")
        BigDecimal quantity,

        @NotBlank(message = "Reason for adjustment is mandatory")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason,

        @Size(max = 100, message = "Reference number must not exceed 100 characters")
        String referenceNumber
) {}
