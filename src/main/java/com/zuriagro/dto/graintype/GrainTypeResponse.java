package com.zuriagro.dto.graintype;

import com.zuriagro.domain.entity.GrainType;

import java.time.Instant;

public record GrainTypeResponse(
        Long id,
        String code,
        String name,
        String description,
        String unitOfMeasure,
        boolean active,
        Instant createdAt
) {
    public static GrainTypeResponse fromEntity(GrainType grainType) {
        return new GrainTypeResponse(
                grainType.getId(),
                grainType.getCode(),
                grainType.getName(),
                grainType.getDescription(),
                grainType.getUnitOfMeasure(),
                grainType.isActive(),
                grainType.getCreatedAt()
        );
    }
}
