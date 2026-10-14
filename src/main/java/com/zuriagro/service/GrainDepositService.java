package com.zuriagro.service;

import com.zuriagro.domain.entity.*;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.deposit.CreateGrainDepositRequest;
import com.zuriagro.dto.deposit.DepositDecisionRequest;
import com.zuriagro.dto.deposit.GrainDepositResponse;
import com.zuriagro.exception.InvalidOperationException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.exception.UnauthorizedException;
import com.zuriagro.repository.GrainDepositRepository;
import com.zuriagro.repository.GrainTypeRepository;
import com.zuriagro.repository.StorageDepotRepository;
import com.zuriagro.repository.UserRepository;
import com.zuriagro.security.SecurityUtils;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GrainDepositService {

    private final GrainDepositRepository grainDepositRepository;
    private final UserRepository userRepository;
    private final StorageDepotRepository storageDepotRepository;
    private final GrainTypeRepository grainTypeRepository;
    private final InventoryService inventoryService;
    private final AuditLogService auditLogService;

    public GrainDepositService(GrainDepositRepository grainDepositRepository,
                               UserRepository userRepository,
                               StorageDepotRepository storageDepotRepository,
                               GrainTypeRepository grainTypeRepository,
                               InventoryService inventoryService,
                               AuditLogService auditLogService) {
        this.grainDepositRepository = grainDepositRepository;
        this.userRepository = userRepository;
        this.storageDepotRepository = storageDepotRepository;
        this.grainTypeRepository = grainTypeRepository;
        this.inventoryService = inventoryService;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public GrainDepositResponse createDeposit(CreateGrainDepositRequest request, String authenticatedUsername) {
        User creator = userRepository.findByUsername(authenticatedUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", authenticatedUsername));

        User farmer;
        boolean isStaff = SecurityUtils.isAdminOrDepotManager();

        if (isStaff) {
            if (request.farmerId() != null) {
                farmer = userRepository.findById(request.farmerId())
                        .orElseThrow(() -> new ResourceNotFoundException("Farmer user not found with id: " + request.farmerId()));
            } else {
                throw new InvalidOperationException("Farmer ID is required when deposit is submitted by staff");
            }
        } else {
            if (request.farmerId() != null && !request.farmerId().equals(creator.getId())) {
                throw new UnauthorizedException("Farmers can only create deposit records for themselves");
            }
            farmer = creator;
        }

        StorageDepot depot = storageDepotRepository.findById(request.depotId())
                .orElseThrow(() -> new ResourceNotFoundException("StorageDepot", "id", request.depotId()));

        if (!depot.isActive()) {
            throw new InvalidOperationException("Cannot record deposit for an inactive storage depot: " + depot.getName());
        }

        GrainType grainType = grainTypeRepository.findById(request.grainTypeId())
                .orElseThrow(() -> new ResourceNotFoundException("GrainType", "id", request.grainTypeId()));

        if (!grainType.isActive()) {
            throw new InvalidOperationException("Cannot record deposit for an inactive grain type: " + grainType.getName());
        }

        String depositRef = "DEP-" + System.currentTimeMillis() + "-" + UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        GrainDeposit deposit = new GrainDeposit();
        deposit.setDepositReference(depositRef);
        deposit.setFarmer(farmer);
        deposit.setDepot(depot);
        deposit.setGrainType(grainType);
        deposit.setQuantity(request.quantity());
        deposit.setUnitOfMeasure(
                (request.unitOfMeasure() != null && !request.unitOfMeasure().isBlank())
                        ? request.unitOfMeasure().toUpperCase()
                        : grainType.getUnitOfMeasure()
        );
        deposit.setStatus(DepositStatus.PENDING);
        deposit.setNotes(request.notes());
        deposit.setCreatedBy(creator);
        deposit.setDepositedAt(Instant.now());

        GrainDeposit saved = grainDepositRepository.save(deposit);

        auditLogService.logAction(
                "GrainDeposit",
                saved.getId(),
                "CREATE",
                creator.getUsername(),
                String.format("Created deposit %s of %s %s for farmer %s at depot %s",
                        depositRef, saved.getQuantity(), saved.getUnitOfMeasure(), farmer.getUsername(), depot.getCode())
        );

        return GrainDepositResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public GrainDepositResponse getDepositById(Long id, String authenticatedUsername) {
        GrainDeposit deposit = grainDepositRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrainDeposit", "id", id));

        assertAccessAllowed(deposit, authenticatedUsername);

        return GrainDepositResponse.fromEntity(deposit);
    }

    @Transactional(readOnly = true)
    public GrainDepositResponse getDepositByReference(String reference, String authenticatedUsername) {
        GrainDeposit deposit = grainDepositRepository.findByDepositReference(reference)
                .orElseThrow(() -> new ResourceNotFoundException("GrainDeposit", "reference", reference));

        assertAccessAllowed(deposit, authenticatedUsername);

        return GrainDepositResponse.fromEntity(deposit);
    }

    @Transactional(readOnly = true)
    public PagedResponse<GrainDepositResponse> getDeposits(Long filterFarmerId,
                                                           Long filterDepotId,
                                                           DepositStatus filterStatus,
                                                           Pageable pageable,
                                                           String authenticatedUsername) {
        Long effectiveFarmerId = filterFarmerId;
        boolean isStaff = SecurityUtils.isAdminOrDepotManager();

        if (!isStaff) {
            User currentUser = userRepository.findByUsername(authenticatedUsername)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", authenticatedUsername));
            effectiveFarmerId = currentUser.getId();
        }

        final Long resolvedFarmerId = effectiveFarmerId;

        Specification<GrainDeposit> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (resolvedFarmerId != null) {
                predicates.add(cb.equal(root.get("farmer").get("id"), resolvedFarmerId));
            }
            if (filterDepotId != null) {
                predicates.add(cb.equal(root.get("depot").get("id"), filterDepotId));
            }
            if (filterStatus != null) {
                predicates.add(cb.equal(root.get("status"), filterStatus));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<GrainDeposit> page = grainDepositRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(GrainDepositResponse::fromEntity));
    }

    @Transactional
    public GrainDepositResponse acceptDeposit(Long id, DepositDecisionRequest request, String reviewerUsername) {
        GrainDeposit deposit = grainDepositRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrainDeposit", "id", id));

        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw new InvalidOperationException(
                    "Deposit " + deposit.getDepositReference() + " cannot be accepted because current status is " + deposit.getStatus()
            );
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", reviewerUsername));

        deposit.setStatus(DepositStatus.ACCEPTED);
        deposit.setReviewedBy(reviewer);
        deposit.setReviewedAt(Instant.now());

        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            String combinedNotes = deposit.getNotes() != null
                    ? deposit.getNotes() + " | Review: " + request.notes()
                    : request.notes();
            deposit.setNotes(combinedNotes);
        }

        // Atomically update inventory and ledger in the same transaction
        inventoryService.recordDepositAcceptance(deposit, reviewer);

        GrainDeposit saved = grainDepositRepository.save(deposit);

        auditLogService.logAction(
                "GrainDeposit",
                saved.getId(),
                "ACCEPT",
                reviewer.getUsername(),
                String.format("Accepted deposit %s: credited %s %s to depot %s",
                        saved.getDepositReference(), saved.getQuantity(), saved.getUnitOfMeasure(), saved.getDepot().getCode())
        );

        return GrainDepositResponse.fromEntity(saved);
    }

    @Transactional
    public GrainDepositResponse rejectDeposit(Long id, DepositDecisionRequest request, String reviewerUsername) {
        GrainDeposit deposit = grainDepositRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrainDeposit", "id", id));

        if (deposit.getStatus() != DepositStatus.PENDING) {
            throw new InvalidOperationException(
                    "Deposit " + deposit.getDepositReference() + " cannot be rejected because current status is " + deposit.getStatus()
            );
        }

        if (request == null || request.rejectionReason() == null || request.rejectionReason().isBlank()) {
            throw new InvalidOperationException("Rejection reason is mandatory when rejecting a deposit");
        }

        User reviewer = userRepository.findByUsername(reviewerUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User", "username", reviewerUsername));

        deposit.setStatus(DepositStatus.REJECTED);
        deposit.setRejectionReason(request.rejectionReason());
        deposit.setReviewedBy(reviewer);
        deposit.setReviewedAt(Instant.now());

        if (request.notes() != null && !request.notes().isBlank()) {
            String combinedNotes = deposit.getNotes() != null
                    ? deposit.getNotes() + " | Review: " + request.notes()
                    : request.notes();
            deposit.setNotes(combinedNotes);
        }

        GrainDeposit saved = grainDepositRepository.save(deposit);

        auditLogService.logAction(
                "GrainDeposit",
                saved.getId(),
                "REJECT",
                reviewer.getUsername(),
                String.format("Rejected deposit %s. Reason: %s", saved.getDepositReference(), request.rejectionReason())
        );

        return GrainDepositResponse.fromEntity(saved);
    }

    private void assertAccessAllowed(GrainDeposit deposit, String authenticatedUsername) {
        if (!SecurityUtils.isAdminOrDepotManager()) {
            if (!deposit.getFarmer().getUsername().equals(authenticatedUsername)) {
                throw new UnauthorizedException("You are not authorized to access this deposit record");
            }
        }
    }
}
