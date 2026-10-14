package com.zuriagro.service;

import com.zuriagro.domain.entity.*;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.inventory.InventoryBalanceResponse;
import com.zuriagro.dto.inventory.InventoryMovementResponse;
import com.zuriagro.dto.inventory.StockAdjustmentRequest;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.InsufficientStockException;
import com.zuriagro.exception.InvalidOperationException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryService {

    private final InventoryBalanceRepository inventoryBalanceRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final StorageDepotRepository storageDepotRepository;
    private final GrainTypeRepository grainTypeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public InventoryService(InventoryBalanceRepository inventoryBalanceRepository,
                            InventoryMovementRepository inventoryMovementRepository,
                            StorageDepotRepository storageDepotRepository,
                            GrainTypeRepository grainTypeRepository,
                            UserRepository userRepository,
                            AuditLogService auditLogService) {
        this.inventoryBalanceRepository = inventoryBalanceRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.storageDepotRepository = storageDepotRepository;
        this.grainTypeRepository = grainTypeRepository;
        this.userRepository = userRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public InventoryMovement recordDepositAcceptance(GrainDeposit deposit, User authorizedBy) {
        if (inventoryMovementRepository.existsBySourceDepositId(deposit.getId())) {
            throw new DuplicateResourceException(
                    "Stock update has already been applied for deposit reference: " + deposit.getDepositReference()
            );
        }

        Long depotId = deposit.getDepot().getId();
        Long grainTypeId = deposit.getGrainType().getId();

        InventoryBalance balance = inventoryBalanceRepository.findByDepotIdAndGrainTypeIdForUpdate(depotId, grainTypeId)
                .orElseGet(() -> {
                    InventoryBalance initBalance = new InventoryBalance(deposit.getDepot(), deposit.getGrainType(), BigDecimal.ZERO);
                    return inventoryBalanceRepository.saveAndFlush(initBalance);
                });

        BigDecimal previousBalance = balance.getQuantity();
        balance.addQuantity(deposit.getQuantity());
        BigDecimal newBalance = balance.getQuantity();
        inventoryBalanceRepository.save(balance);

        String movementRef = "MOV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();

        InventoryMovement movement = new InventoryMovement();
        movement.setMovementReference(movementRef);
        movement.setDepot(deposit.getDepot());
        movement.setGrainType(deposit.getGrainType());
        movement.setMovementType(MovementType.DEPOSIT_ACCEPTANCE);
        movement.setQuantity(deposit.getQuantity());
        movement.setUnitOfMeasure(deposit.getUnitOfMeasure());
        movement.setPreviousBalance(previousBalance);
        movement.setNewBalance(newBalance);
        movement.setSourceDeposit(deposit);
        movement.setReferenceNumber(deposit.getDepositReference());
        movement.setReason("Deposit acceptance for reference " + deposit.getDepositReference());
        movement.setAuthorizedBy(authorizedBy);

        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        auditLogService.logAction(
                "InventoryBalance",
                balance.getId(),
                "STOCK_INCREMENT",
                authorizedBy.getUsername(),
                String.format("Added %s %s to depot %s for deposit %s",
                        deposit.getQuantity(), deposit.getUnitOfMeasure(), deposit.getDepot().getCode(), deposit.getDepositReference())
        );

        return savedMovement;
    }

    @Transactional
    public InventoryMovementResponse adjustStock(StockAdjustmentRequest request, String authorizedByUsername) {
        User authorizedBy = userRepository.findByUsername(authorizedByUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", authorizedByUsername));

        StorageDepot depot = storageDepotRepository.findById(request.depotId())
                .orElseThrow(() -> new ResourceNotFoundException("StorageDepot", "id", request.depotId()));

        GrainType grainType = grainTypeRepository.findById(request.grainTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("GrainType", "id", request.grainTypeId()));

        InventoryBalance balance = inventoryBalanceRepository.findByDepotIdAndGrainTypeIdForUpdate(depot.getId(), grainType.getId())
                .orElseGet(() -> {
                    if (request.movementType() == MovementType.ADJUSTMENT_SUBTRACT) {
                        throw new InsufficientStockException("Cannot subtract stock: no inventory balance exists for depot "
                                + depot.getCode() + " and grain type " + grainType.getCode());
                    }
                    InventoryBalance initBalance = new InventoryBalance(depot, grainType, BigDecimal.ZERO);
                    return inventoryBalanceRepository.saveAndFlush(initBalance);
                });

        BigDecimal previousBalance = balance.getQuantity();

        if (request.movementType() == MovementType.ADJUSTMENT_ADD) {
            balance.addQuantity(request.quantity());
        } else if (request.movementType() == MovementType.ADJUSTMENT_SUBTRACT) {
            if (balance.getAvailableQuantity().compareTo(request.quantity()) < 0) {
                throw new InsufficientStockException(String.format(
                        "Insufficient stock to subtract %s %s. Available quantity is %s %s",
                        request.quantity(), grainType.getUnitOfMeasure(),
                        balance.getAvailableQuantity(), grainType.getUnitOfMeasure()
                ));
            }
            balance.subtractQuantity(request.quantity());
        } else {
            throw new InvalidOperationException("Unsupported adjustment movement type: " + request.movementType());
        }

        BigDecimal newBalance = balance.getQuantity();
        inventoryBalanceRepository.save(balance);

        String movementRef = "MOV-ADJ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase();

        InventoryMovement movement = new InventoryMovement();
        movement.setMovementReference(movementRef);
        movement.setDepot(depot);
        movement.setGrainType(grainType);
        movement.setMovementType(request.movementType());
        movement.setQuantity(request.quantity());
        movement.setUnitOfMeasure(grainType.getUnitOfMeasure());
        movement.setPreviousBalance(previousBalance);
        movement.setNewBalance(newBalance);
        movement.setReferenceNumber(request.referenceNumber());
        movement.setReason(request.reason());
        movement.setAuthorizedBy(authorizedBy);

        InventoryMovement savedMovement = inventoryMovementRepository.save(movement);

        auditLogService.logAction(
                "InventoryBalance",
                balance.getId(),
                "STOCK_ADJUSTMENT",
                authorizedBy.getUsername(),
                String.format("Adjusted stock (%s) by %s %s for depot %s. Reason: %s",
                        request.movementType(), request.quantity(), grainType.getUnitOfMeasure(), depot.getCode(), request.reason())
        );

        return InventoryMovementResponse.fromEntity(savedMovement);
    }

    @Transactional(readOnly = true)
    public List<InventoryBalanceResponse> getBalancesByDepot(Long depotId) {
        if (!storageDepotRepository.existsById(depotId)) {
            throw new ResourceNotFoundException("StorageDepot", "id", depotId);
        }
        return inventoryBalanceRepository.findByDepotId(depotId).stream()
                .map(InventoryBalanceResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public InventoryBalanceResponse getBalance(Long depotId, Long grainTypeId) {
        InventoryBalance balance = inventoryBalanceRepository.findByDepotIdAndGrainTypeId(depotId, grainTypeId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "InventoryBalance not found for depot ID " + depotId + " and grain type ID " + grainTypeId
                ));
        return InventoryBalanceResponse.fromEntity(balance);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InventoryMovementResponse> getMovements(Long depotId, Long grainTypeId, Pageable pageable) {
        Page<InventoryMovement> page;
        if (depotId != null && grainTypeId != null) {
            page = inventoryMovementRepository.findByDepotIdAndGrainTypeId(depotId, grainTypeId, pageable);
        } else if (depotId != null) {
            page = inventoryMovementRepository.findByDepotId(depotId, pageable);
        } else if (grainTypeId != null) {
            page = inventoryMovementRepository.findByGrainTypeId(grainTypeId, pageable);
        } else {
            page = inventoryMovementRepository.findAll(pageable);
        }
        return PagedResponse.from(page.map(InventoryMovementResponse::fromEntity));
    }
}
