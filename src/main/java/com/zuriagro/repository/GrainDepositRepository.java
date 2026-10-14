package com.zuriagro.repository;

import com.zuriagro.domain.entity.DepositStatus;
import com.zuriagro.domain.entity.GrainDeposit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GrainDepositRepository extends JpaRepository<GrainDeposit, Long>, JpaSpecificationExecutor<GrainDeposit> {

    Optional<GrainDeposit> findByDepositReference(String depositReference);

    boolean existsByDepositReference(String depositReference);

    Page<GrainDeposit> findByFarmerId(Long farmerId, Pageable pageable);

    Page<GrainDeposit> findByDepotId(Long depotId, Pageable pageable);

    Page<GrainDeposit> findByStatus(DepositStatus status, Pageable pageable);

    Page<GrainDeposit> findByFarmerIdAndStatus(Long farmerId, DepositStatus status, Pageable pageable);

    long countByStatus(DepositStatus status);
}
