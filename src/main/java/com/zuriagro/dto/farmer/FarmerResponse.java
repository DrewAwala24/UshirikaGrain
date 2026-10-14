package com.zuriagro.dto.farmer;

import com.zuriagro.domain.entity.DepositStatus;
import com.zuriagro.domain.entity.Farmer;
import com.zuriagro.domain.entity.FarmerStatus;
import com.zuriagro.domain.entity.GrainDeposit;

import java.math.BigDecimal;
import java.time.Instant;

public record FarmerResponse(
        Long id,
        String farmerCode,
        String fullName,
        String nationalId,
        String phoneNumber,
        String email,
        String county,
        String subCounty,
        String ward,
        String location,
        FarmerStatus status,
        Long userId,
        String registeredByUsername,
        long totalDeposits,
        BigDecimal totalQuantityDepositedKg,
        Instant createdAt,
        Instant updatedAt
) {
    public static FarmerResponse fromEntity(Farmer farmer) {
        long depositsCount = farmer.getDeposits() != null ? farmer.getDeposits().size() : 0;
        BigDecimal totalKg = BigDecimal.ZERO;
        if (farmer.getDeposits() != null) {
            for (GrainDeposit d : farmer.getDeposits()) {
                if (d.getStatus() == DepositStatus.ACCEPTED && d.getQuantity() != null) {
                    totalKg = totalKg.add(d.getQuantity());
                }
            }
        }

        return new FarmerResponse(
                farmer.getId(),
                farmer.getFarmerCode(),
                farmer.getFullName(),
                farmer.getNationalId(),
                farmer.getPhoneNumber(),
                farmer.getEmail(),
                farmer.getCounty(),
                farmer.getSubCounty(),
                farmer.getWard(),
                farmer.getLocation(),
                farmer.getStatus(),
                farmer.getUser() != null ? farmer.getUser().getId() : null,
                farmer.getRegisteredBy() != null ? farmer.getRegisteredBy().getUsername() : null,
                depositsCount,
                totalKg,
                farmer.getCreatedAt(),
                farmer.getUpdatedAt()
        );
    }
}
