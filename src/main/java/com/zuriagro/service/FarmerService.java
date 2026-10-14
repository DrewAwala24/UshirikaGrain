package com.zuriagro.service;

import com.zuriagro.domain.entity.Farmer;
import com.zuriagro.domain.entity.FarmerStatus;
import com.zuriagro.domain.entity.User;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.deposit.GrainDepositResponse;
import com.zuriagro.dto.farmer.CreateFarmerRequest;
import com.zuriagro.dto.farmer.FarmerDetailResponse;
import com.zuriagro.dto.farmer.FarmerResponse;
import com.zuriagro.dto.farmer.UpdateFarmerRequest;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.repository.FarmerRepository;
import com.zuriagro.repository.GrainDepositRepository;
import com.zuriagro.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FarmerService {

    private final FarmerRepository farmerRepository;
    private final UserRepository userRepository;
    private final GrainDepositRepository grainDepositRepository;
    private final AuditLogService auditLogService;

    public FarmerService(FarmerRepository farmerRepository,
                         UserRepository userRepository,
                         GrainDepositRepository grainDepositRepository,
                         AuditLogService auditLogService) {
        this.farmerRepository = farmerRepository;
        this.userRepository = userRepository;
        this.grainDepositRepository = grainDepositRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public FarmerResponse createFarmer(CreateFarmerRequest request, String authenticatedUsername) {
        // Enforce phone uniqueness
        if (farmerRepository.existsByPhoneNumber(request.phoneNumber())) {
            throw new DuplicateResourceException("A farmer with phone number " + request.phoneNumber() + " already exists");
        }

        // Enforce national ID uniqueness if provided
        if (request.nationalId() != null && !request.nationalId().isBlank()) {
            if (farmerRepository.existsByNationalId(request.nationalId().trim())) {
                throw new DuplicateResourceException("A farmer with National ID " + request.nationalId() + " already exists");
            }
        }

        // Generate unique farmer code: FRM-YYYY-XXXX
        String farmerCode = generateFarmerCode();

        Farmer farmer = new Farmer(
                farmerCode,
                request.fullName().trim(),
                request.nationalId() != null ? request.nationalId().trim() : null,
                request.phoneNumber().trim(),
                request.email() != null ? request.email().trim().toLowerCase() : null,
                request.county() != null ? request.county().trim() : "Trans Nzoia",
                request.subCounty() != null ? request.subCounty().trim() : null,
                request.ward() != null ? request.ward().trim() : null,
                request.location() != null ? request.location().trim() : null
        );

        if (authenticatedUsername != null && !authenticatedUsername.equals("anonymousUser")) {
            userRepository.findByUsername(authenticatedUsername).ifPresent(farmer::setRegisteredBy);
        }

        if (request.userId() != null) {
            User linkedUser = userRepository.findById(request.userId())
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", request.userId()));
            farmer.setUser(linkedUser);
        }

        Farmer saved = farmerRepository.save(farmer);

        auditLogService.logAction(
                "Farmer",
                saved.getId(),
                "CREATE",
                authenticatedUsername != null ? authenticatedUsername : "SYSTEM",
                String.format("Registered farmer %s (%s)", saved.getFullName(), saved.getFarmerCode())
        );

        return FarmerResponse.fromEntity(saved);
    }

    @Transactional
    public FarmerResponse updateFarmer(Long id, UpdateFarmerRequest request, String authenticatedUsername) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", "id", id));

        if (request.fullName() != null && !request.fullName().isBlank()) {
            farmer.setFullName(request.fullName().trim());
        }

        if (request.phoneNumber() != null && !request.phoneNumber().isBlank()) {
            String newPhone = request.phoneNumber().trim();
            if (!newPhone.equals(farmer.getPhoneNumber())) {
                if (farmerRepository.existsByPhoneNumber(newPhone)) {
                    throw new DuplicateResourceException("A farmer with phone number " + newPhone + " already exists");
                }
                farmer.setPhoneNumber(newPhone);
            }
        }

        if (request.nationalId() != null && !request.nationalId().isBlank()) {
            String newId = request.nationalId().trim();
            if (!newId.equals(farmer.getNationalId())) {
                if (farmerRepository.existsByNationalId(newId)) {
                    throw new DuplicateResourceException("A farmer with National ID " + newId + " already exists");
                }
                farmer.setNationalId(newId);
            }
        }

        if (request.email() != null) {
            farmer.setEmail(request.email().isBlank() ? null : request.email().trim().toLowerCase());
        }
        if (request.county() != null) farmer.setCounty(request.county().trim());
        if (request.subCounty() != null) farmer.setSubCounty(request.subCounty().trim());
        if (request.ward() != null) farmer.setWard(request.ward().trim());
        if (request.location() != null) farmer.setLocation(request.location().trim());
        if (request.status() != null) farmer.setStatus(request.status());

        Farmer saved = farmerRepository.save(farmer);

        auditLogService.logAction(
                "Farmer",
                saved.getId(),
                "UPDATE",
                authenticatedUsername != null ? authenticatedUsername : "SYSTEM",
                String.format("Updated farmer record %s (%s)", saved.getFullName(), saved.getFarmerCode())
        );

        return FarmerResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public FarmerResponse getFarmerById(Long id) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", "id", id));
        return FarmerResponse.fromEntity(farmer);
    }

    @Transactional(readOnly = true)
    public FarmerDetailResponse getFarmerDetail(Long id) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", "id", id));

        List<GrainDepositResponse> deposits = farmer.getDeposits() != null
                ? farmer.getDeposits().stream().map(GrainDepositResponse::fromEntity).toList()
                : List.of();

        return FarmerDetailResponse.of(farmer, deposits);
    }

    @Transactional(readOnly = true)
    public FarmerResponse getFarmerByCode(String code) {
        Farmer farmer = farmerRepository.findByFarmerCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", "farmerCode", code));
        return FarmerResponse.fromEntity(farmer);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FarmerResponse> getFarmers(String search, FarmerStatus status, Pageable pageable) {
        Page<Farmer> page = farmerRepository.searchFarmers(
                search != null ? search.trim() : null,
                status,
                pageable
        );
        return PagedResponse.from(page.map(FarmerResponse::fromEntity));
    }

    @Transactional
    public FarmerResponse setFarmerStatus(Long id, FarmerStatus status, String authenticatedUsername) {
        Farmer farmer = farmerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer", "id", id));

        farmer.setStatus(status);
        Farmer saved = farmerRepository.save(farmer);

        auditLogService.logAction(
                "Farmer",
                saved.getId(),
                "STATUS_CHANGE",
                authenticatedUsername != null ? authenticatedUsername : "SYSTEM",
                String.format("Changed status of farmer %s to %s", saved.getFarmerCode(), status)
        );

        return FarmerResponse.fromEntity(saved);
    }

    private synchronized String generateFarmerCode() {
        int year = Year.now().getValue();
        long count = farmerRepository.count() + 1;
        String code;
        do {
            code = String.format("FRM-%d-%04d", year, count++);
        } while (farmerRepository.existsByFarmerCode(code));
        return code;
    }
}
