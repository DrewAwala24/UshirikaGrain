package com.zuriagro.repository;

import com.zuriagro.domain.entity.StorageDepot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface StorageDepotRepository extends JpaRepository<StorageDepot, Long> {
    Optional<StorageDepot> findByCode(String code);
    boolean existsByCode(String code);
    List<StorageDepot> findByActiveTrue();
}
