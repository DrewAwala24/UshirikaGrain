package com.zuriagro.repository;

import com.zuriagro.domain.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {

    Optional<InventoryMovement> findByMovementReference(String movementReference);

    boolean existsBySourceDepositId(Long sourceDepositId);

    Page<InventoryMovement> findByDepotId(Long depotId, Pageable pageable);

    Page<InventoryMovement> findByGrainTypeId(Long grainTypeId, Pageable pageable);

    Page<InventoryMovement> findByDepotIdAndGrainTypeId(Long depotId, Long grainTypeId, Pageable pageable);
}
