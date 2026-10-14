package com.zuriagro.service;

import com.zuriagro.domain.entity.*;
import com.zuriagro.dto.inventory.InventoryMovementResponse;
import com.zuriagro.dto.inventory.StockAdjustmentRequest;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.InsufficientStockException;
import com.zuriagro.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Mock
    private InventoryMovementRepository inventoryMovementRepository;

    @Mock
    private StorageDepotRepository storageDepotRepository;

    @Mock
    private GrainTypeRepository grainTypeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private InventoryService inventoryService;

    private StorageDepot depot;
    private GrainType grainType;
    private User adminUser;
    private GrainDeposit deposit;
    private InventoryBalance existingBalance;

    @BeforeEach
    void setUp() {
        depot = new StorageDepot("ELD-01", "Eldoret Silo", "Eldoret", new BigDecimal("50000.00"));
        depot.setId(1L);

        grainType = new GrainType("MAIZE", "White Maize", "Maize grain", "KG");
        grainType.setId(1L);

        adminUser = new User("admin", "admin@test.com", "hash", "Admin User", "0700");
        adminUser.setId(1L);

        deposit = new GrainDeposit();
        deposit.setId(50L);
        deposit.setDepositReference("DEP-2026-001");
        deposit.setDepot(depot);
        deposit.setGrainType(grainType);
        deposit.setQuantity(new BigDecimal("1000.00"));
        deposit.setUnitOfMeasure("KG");

        existingBalance = new InventoryBalance(depot, grainType, new BigDecimal("4000.00"));
        existingBalance.setId(10L);
    }

    @Test
    void recordDepositAcceptance_Success_IncrementsBalanceAndSavesMovement() {
        when(inventoryMovementRepository.existsBySourceDepositId(50L)).thenReturn(false);
        when(inventoryBalanceRepository.findByDepotIdAndGrainTypeIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(existingBalance));
        when(inventoryMovementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryMovement movement = inventoryService.recordDepositAcceptance(deposit, adminUser);

        assertThat(movement).isNotNull();
        assertThat(movement.getMovementType()).isEqualTo(MovementType.DEPOSIT_ACCEPTANCE);
        assertThat(movement.getQuantity()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(movement.getPreviousBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(movement.getNewBalance()).isEqualByComparingTo(new BigDecimal("5000.00"));
        assertThat(existingBalance.getQuantity()).isEqualByComparingTo(new BigDecimal("5000.00"));

        verify(inventoryBalanceRepository).save(existingBalance);
        verify(inventoryMovementRepository).save(any(InventoryMovement.class));
        verify(auditLogService).logAction(eq("InventoryBalance"), eq(10L), eq("STOCK_INCREMENT"), eq("admin"), anyString());
    }

    @Test
    void recordDepositAcceptance_Duplicate_ThrowsDuplicateResourceException() {
        when(inventoryMovementRepository.existsBySourceDepositId(50L)).thenReturn(true);

        assertThatThrownBy(() -> inventoryService.recordDepositAcceptance(deposit, adminUser))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("Stock update has already been applied for deposit reference: DEP-2026-001");

        verify(inventoryBalanceRepository, never()).save(any());
        verify(inventoryMovementRepository, never()).save(any());
    }

    @Test
    void adjustStock_Add_Success() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                1L, 1L, MovementType.ADJUSTMENT_ADD, new BigDecimal("500.00"), "Annual reconciliation surplus", "REF-001"
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(storageDepotRepository.findById(1L)).thenReturn(Optional.of(depot));
        when(grainTypeRepository.findById(1L)).thenReturn(Optional.of(grainType));
        when(inventoryBalanceRepository.findByDepotIdAndGrainTypeIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(existingBalance));
        when(inventoryMovementRepository.save(any(InventoryMovement.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InventoryMovementResponse response = inventoryService.adjustStock(request, "admin");

        assertThat(response).isNotNull();
        assertThat(response.movementType()).isEqualTo(MovementType.ADJUSTMENT_ADD);
        assertThat(response.previousBalance()).isEqualByComparingTo(new BigDecimal("4000.00"));
        assertThat(response.newBalance()).isEqualByComparingTo(new BigDecimal("4500.00"));
        assertThat(existingBalance.getQuantity()).isEqualByComparingTo(new BigDecimal("4500.00"));
    }

    @Test
    void adjustStock_Subtract_ThrowsInsufficientStock_WhenExceedingBalance() {
        StockAdjustmentRequest request = new StockAdjustmentRequest(
                1L, 1L, MovementType.ADJUSTMENT_SUBTRACT, new BigDecimal("10000.00"), "Discrepancy write-off", "REF-002"
        );

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));
        when(storageDepotRepository.findById(1L)).thenReturn(Optional.of(depot));
        when(grainTypeRepository.findById(1L)).thenReturn(Optional.of(grainType));
        when(inventoryBalanceRepository.findByDepotIdAndGrainTypeIdForUpdate(1L, 1L))
                .thenReturn(Optional.of(existingBalance));

        assertThatThrownBy(() -> inventoryService.adjustStock(request, "admin"))
                .isInstanceOf(InsufficientStockException.class)
                .hasMessageContaining("Insufficient stock to subtract");

        verify(inventoryBalanceRepository, never()).save(any());
    }
}
