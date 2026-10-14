package com.zuriagro.service;

import com.zuriagro.domain.entity.DepositStatus;
import com.zuriagro.domain.entity.FarmerStatus;
import com.zuriagro.domain.entity.GrainType;
import com.zuriagro.domain.entity.StorageDepot;
import com.zuriagro.dto.dashboard.DashboardSummaryResponse;
import com.zuriagro.repository.FarmerRepository;
import com.zuriagro.repository.GrainDepositRepository;
import com.zuriagro.repository.GrainTypeRepository;
import com.zuriagro.repository.InventoryBalanceRepository;
import com.zuriagro.repository.StorageDepotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class DashboardService {

    private static final BigDecimal KG_PER_METRIC_TONNE = new BigDecimal("1000");
    private static final BigDecimal KG_PER_BAG = new BigDecimal("90");

    private final GrainDepositRepository depositRepository;
    private final StorageDepotRepository depotRepository;
    private final InventoryBalanceRepository balanceRepository;
    private final FarmerRepository farmerRepository;
    private final GrainTypeRepository grainTypeRepository;

    public DashboardService(GrainDepositRepository depositRepository,
                            StorageDepotRepository depotRepository,
                            InventoryBalanceRepository balanceRepository,
                            FarmerRepository farmerRepository,
                            GrainTypeRepository grainTypeRepository) {
        this.depositRepository = depositRepository;
        this.depotRepository = depotRepository;
        this.balanceRepository = balanceRepository;
        this.farmerRepository = farmerRepository;
        this.grainTypeRepository = grainTypeRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        long totalDeposits   = depositRepository.count();
        long pendingDeposits = depositRepository.countByStatus(DepositStatus.PENDING);
        long acceptedDeposits = depositRepository.countByStatus(DepositStatus.ACCEPTED);
        long rejectedDeposits = depositRepository.countByStatus(DepositStatus.REJECTED);

        Instant startOfToday = Instant.now().truncatedTo(ChronoUnit.DAYS);
        long todayDeposits = depositRepository.countByCreatedAtAfter(startOfToday);

        List<StorageDepot> activeDepots = depotRepository.findByActiveTrue();

        BigDecimal totalStockKg = balanceRepository.sumAllQuantities()
                .orElse(BigDecimal.ZERO);

        long totalFarmers = farmerRepository.count();
        long activeFarmers = farmerRepository.countByStatus(FarmerStatus.ACTIVE);
        long inactiveFarmers = farmerRepository.countByStatus(FarmerStatus.INACTIVE);

        List<DashboardSummaryResponse.DepotSummary> depotSummaries = activeDepots.stream()
                .map(depot -> {
                    BigDecimal stockKg = balanceRepository.sumQuantitiesByDepotId(depot.getId())
                            .orElse(BigDecimal.ZERO);
                    BigDecimal capacityKg = depot.getCapacityMetricTonnes()
                            .multiply(KG_PER_METRIC_TONNE);
                    double utilization = capacityKg.compareTo(BigDecimal.ZERO) == 0 ? 0.0
                            : stockKg.divide(capacityKg, 4, RoundingMode.HALF_UP)
                                     .doubleValue() * 100.0;
                    return new DashboardSummaryResponse.DepotSummary(
                            depot.getId(),
                            depot.getCode(),
                            depot.getName(),
                            depot.getLocation(),
                            depot.getCapacityMetricTonnes(),
                            stockKg,
                            Math.min(100.0, utilization)
                    );
                })
                .toList();

        List<GrainType> activeGrainTypes = grainTypeRepository.findByActiveTrue();
        List<DashboardSummaryResponse.GrainStockSummary> grainStockSummaries = activeGrainTypes.stream()
                .map(gt -> {
                    BigDecimal stockKg = balanceRepository.sumQuantitiesByGrainTypeId(gt.getId())
                            .orElse(BigDecimal.ZERO);
                    long bags = stockKg.divide(KG_PER_BAG, 0, RoundingMode.HALF_UP).longValue();
                    return new DashboardSummaryResponse.GrainStockSummary(
                            gt.getId(),
                            gt.getCode(),
                            gt.getName(),
                            stockKg,
                            bags
                    );
                })
                .toList();

        return new DashboardSummaryResponse(
                totalDeposits,
                pendingDeposits,
                acceptedDeposits,
                rejectedDeposits,
                todayDeposits,
                totalStockKg,
                activeDepots.size(),
                totalFarmers,
                activeFarmers,
                inactiveFarmers,
                depotSummaries,
                grainStockSummaries
        );
    }
}
