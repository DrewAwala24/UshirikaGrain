package com.zuriagro.dto.deposit;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateGrainDepositRequest(
        Long farmerId,

        @NotNull(message = "Depot ID is required")
        Long depotId,

        @NotNull(message = "Grain type ID is required")
        Long grainTypeId,

        @NotNull(message = "Quantity is required")
        @DecimalMin(value = "0.01", message = "Quantity must be positive and greater than zero")
        BigDecimal quantity,

        @Size(max = 20, message = "Unit of measure must not exceed 20 characters")
        String unitOfMeasure,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
