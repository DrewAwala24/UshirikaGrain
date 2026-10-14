package com.zuriagro.repository;

import com.zuriagro.domain.entity.GrainType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GrainTypeRepository extends JpaRepository<GrainType, Long> {
    Optional<GrainType> findByCode(String code);
    boolean existsByCode(String code);
    List<GrainType> findByActiveTrue();
}
