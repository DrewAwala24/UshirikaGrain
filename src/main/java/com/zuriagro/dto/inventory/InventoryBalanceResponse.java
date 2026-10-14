package com.zuriagro.dto.inventory;

import com.zuriagro.domain.entity.InventoryBalance;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryBalanceResponse(
        Long id,
        Long depotId,
        String depotCode,
        String depotName,
        Long grainTypeId,
        String grainTypeCode,
        String grainTypeName,
        BigDecimal quantity,
        BigDecimal reservedQuantity,
        BigDecimal availableQuantity,
        String unitOfMeasure,
        Instant lastUpdated
) {
    public static InventoryBalanceResponse fromEntity(InventoryBalance balance) {
        return new InventoryBalanceResponse(
                balance.getId(),
                balance.getDepot().getId(),
                balance.getDepot().getCode(),
                balance.getDepot().getName(),
                balance.getGrainType().getId(),
                balance.getGrainType().getCode(),
                balance.getGrainType().getName(),
                balance.getQuantity(),
                balance.getReservedQuantity(),
                balance.getAvailableQuantity(),
                balance.getGrainType().getUnitOfMeasure(),
                balance.getLastUpdated()
        );
    }
}
