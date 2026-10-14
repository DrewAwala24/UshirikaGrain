package com.zuriagro.dto.deposit;

import jakarta.validation.constraints.DecimalMax;
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
        String notes,

        @DecimalMin(value = "0.0", message = "Moisture percentage must be zero or positive")
        @DecimalMax(value = "100.0", message = "Moisture percentage must not exceed 100")
        BigDecimal moisturePercentage,

        @Size(max = 150, message = "Farmer name must not exceed 150 characters")
        String farmerName,

        @Size(max = 30, message = "Farmer phone must not exceed 30 characters")
        String farmerPhone,

        Long farmerRecordId
) {
    public CreateGrainDepositRequest(Long farmerId, Long depotId, Long grainTypeId, BigDecimal quantity, String unitOfMeasure, String notes) {
        this(farmerId, depotId, grainTypeId, quantity, unitOfMeasure, notes, null, null, null, null);
    }

    public CreateGrainDepositRequest(Long farmerId, Long depotId, Long grainTypeId, BigDecimal quantity, String unitOfMeasure, String notes, BigDecimal moisturePercentage, String farmerName, String farmerPhone) {
        this(farmerId, depotId, grainTypeId, quantity, unitOfMeasure, notes, moisturePercentage, farmerName, farmerPhone, null);
    }
}
