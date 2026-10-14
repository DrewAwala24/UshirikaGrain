package com.zuriagro.dto.farmer;

import com.zuriagro.domain.entity.Farmer;
import com.zuriagro.dto.deposit.GrainDepositResponse;

import java.util.List;

public record FarmerDetailResponse(
        FarmerResponse farmer,
        List<GrainDepositResponse> recentDeposits
) {
    public static FarmerDetailResponse of(Farmer farmer, List<GrainDepositResponse> deposits) {
        return new FarmerDetailResponse(FarmerResponse.fromEntity(farmer), deposits);
    }
}
