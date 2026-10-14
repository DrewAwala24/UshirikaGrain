package com.zuriagro.dto.deposit;

import com.zuriagro.domain.entity.DepositStatus;
import com.zuriagro.domain.entity.GrainDeposit;

import java.math.BigDecimal;
import java.time.Instant;

public record GrainDepositResponse(
        Long id,
        String depositReference,
        Long farmerId,
        String farmerUsername,
        String farmerFullName,
        Long depotId,
        String depotCode,
        String depotName,
        Long grainTypeId,
        String grainTypeCode,
        String grainTypeName,
        BigDecimal quantity,
        String unitOfMeasure,
        DepositStatus status,
        String rejectionReason,
        String notes,
        String createdByUsername,
        String reviewedByUsername,
        Instant depositedAt,
        Instant reviewedAt,
        Instant createdAt,
        Instant updatedAt
) {
    public static GrainDepositResponse fromEntity(GrainDeposit deposit) {
        return new GrainDepositResponse(
                deposit.getId(),
                deposit.getDepositReference(),
                deposit.getFarmer().getId(),
                deposit.getFarmer().getUsername(),
                deposit.getFarmer().getFullName(),
                deposit.getDepot().getId(),
                deposit.getDepot().getCode(),
                deposit.getDepot().getName(),
                deposit.getGrainType().getId(),
                deposit.getGrainType().getCode(),
                deposit.getGrainType().getName(),
                deposit.getQuantity(),
                deposit.getUnitOfMeasure(),
                deposit.getStatus(),
                deposit.getRejectionReason(),
                deposit.getNotes(),
                deposit.getCreatedBy() != null ? deposit.getCreatedBy().getUsername() : null,
                deposit.getReviewedBy() != null ? deposit.getReviewedBy().getUsername() : null,
                deposit.getDepositedAt(),
                deposit.getReviewedAt(),
                deposit.getCreatedAt(),
                deposit.getUpdatedAt()
        );
    }
}
