package com.zuriagro.dto.inventory;

import com.zuriagro.domain.entity.InventoryMovement;
import com.zuriagro.domain.entity.MovementType;

import java.math.BigDecimal;
import java.time.Instant;

public record InventoryMovementResponse(
        Long id,
        String movementReference,
        Long depotId,
        String depotCode,
        String depotName,
        Long grainTypeId,
        String grainTypeCode,
        String grainTypeName,
        MovementType movementType,
        BigDecimal quantity,
        String unitOfMeasure,
        BigDecimal previousBalance,
        BigDecimal newBalance,
        Long sourceDepositId,
        String sourceDepositReference,
        String referenceNumber,
        String reason,
        String authorizedByUsername,
        Instant createdAt
) {
    public static InventoryMovementResponse fromEntity(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getMovementReference(),
                movement.getDepot().getId(),
                movement.getDepot().getCode(),
                movement.getDepot().getName(),
                movement.getGrainType().getId(),
                movement.getGrainType().getCode(),
                movement.getGrainType().getName(),
                movement.getMovementType(),
                movement.getQuantity(),
                movement.getUnitOfMeasure(),
                movement.getPreviousBalance(),
                movement.getNewBalance(),
                movement.getSourceDeposit() != null ? movement.getSourceDeposit().getId() : null,
                movement.getSourceDeposit() != null ? movement.getSourceDeposit().getDepositReference() : null,
                movement.getReferenceNumber(),
                movement.getReason(),
                movement.getAuthorizedBy().getUsername(),
                movement.getCreatedAt()
        );
    }
}
