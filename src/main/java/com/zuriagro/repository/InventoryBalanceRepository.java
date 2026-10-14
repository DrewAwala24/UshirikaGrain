package com.zuriagro.repository;

import com.zuriagro.domain.entity.GrainType;
import com.zuriagro.domain.entity.InventoryBalance;
import com.zuriagro.domain.entity.StorageDepot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InventoryBalanceRepository extends JpaRepository<InventoryBalance, Long> {

    Optional<InventoryBalance> findByDepotAndGrainType(StorageDepot depot, GrainType grainType);

    Optional<InventoryBalance> findByDepotIdAndGrainTypeId(Long depotId, Long grainTypeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT ib FROM InventoryBalance ib WHERE ib.depot.id = :depotId AND ib.grainType.id = :grainTypeId")
    Optional<InventoryBalance> findByDepotIdAndGrainTypeIdForUpdate(
        @Param("depotId") Long depotId,
        @Param("grainTypeId") Long grainTypeId
    );

    List<InventoryBalance> findByDepotId(Long depotId);

    List<InventoryBalance> findByGrainTypeId(Long grainTypeId);
}
