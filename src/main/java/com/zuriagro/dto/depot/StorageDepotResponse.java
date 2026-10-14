package com.zuriagro.dto.depot;

import com.zuriagro.domain.entity.StorageDepot;

import java.math.BigDecimal;
import java.time.Instant;

public record StorageDepotResponse(
        Long id,
        String code,
        String name,
        String location,
        BigDecimal capacityMetricTonnes,
        boolean active,
        Instant createdAt
) {
    public static StorageDepotResponse fromEntity(StorageDepot depot) {
        return new StorageDepotResponse(
                depot.getId(),
                depot.getCode(),
                depot.getName(),
                depot.getLocation(),
                depot.getCapacityMetricTonnes(),
                depot.isActive(),
                depot.getCreatedAt()
        );
    }
}
