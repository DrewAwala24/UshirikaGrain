package com.zuriagro.repository;

import com.zuriagro.domain.entity.Farmer;
import com.zuriagro.domain.entity.FarmerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FarmerRepository extends JpaRepository<Farmer, Long>, JpaSpecificationExecutor<Farmer> {

    Optional<Farmer> findByFarmerCode(String farmerCode);

    Optional<Farmer> findByNationalId(String nationalId);

    Optional<Farmer> findByPhoneNumber(String phoneNumber);

    Optional<Farmer> findByUserId(Long userId);

    boolean existsByFarmerCode(String farmerCode);

    boolean existsByNationalId(String nationalId);

    boolean existsByPhoneNumber(String phoneNumber);

    Page<Farmer> findByStatus(FarmerStatus status, Pageable pageable);

    long countByStatus(FarmerStatus status);

    @Query("SELECT f FROM Farmer f WHERE " +
           "(:search IS NULL OR :search = '' OR " +
           "LOWER(f.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.farmerCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "f.phoneNumber LIKE CONCAT('%', :search, '%') OR " +
           "f.nationalId LIKE CONCAT('%', :search, '%')) AND " +
           "(:status IS NULL OR f.status = :status)")
    Page<Farmer> searchFarmers(@Param("search") String search,
                               @Param("status") FarmerStatus status,
                               Pageable pageable);
}
