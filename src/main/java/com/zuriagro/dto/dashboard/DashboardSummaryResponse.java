package com.zuriagro.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardSummaryResponse(
        long totalDeposits,
        long pendingDeposits,
        long acceptedDeposits,
        long rejectedDeposits,
        long todayDeposits,
        BigDecimal totalStockKg,
        int activeDepots,
        long totalFarmers,
        long activeFarmers,
        long inactiveFarmers,
        List<DepotSummary> depots,
        List<GrainStockSummary> grainStock
) {
    public record DepotSummary(
            Long id,
            String code,
            String name,
            String location,
            BigDecimal capacityMetricTonnes,
            BigDecimal currentStockKg,
            double utilizationPercent
    ) {}

    public record GrainStockSummary(
            Long id,
            String code,
            String name,
            BigDecimal stockKg,
            long bags
    ) {}
}
