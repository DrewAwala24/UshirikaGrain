package com.zuriagro.service;

import com.zuriagro.domain.entity.StorageDepot;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.depot.CreateStorageDepotRequest;
import com.zuriagro.dto.depot.StorageDepotResponse;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.repository.StorageDepotRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StorageDepotService {

    private final StorageDepotRepository storageDepotRepository;

    public StorageDepotService(StorageDepotRepository storageDepotRepository) {
        this.storageDepotRepository = storageDepotRepository;
    }

    @Transactional
    public StorageDepotResponse createDepot(CreateStorageDepotRequest request) {
        if (storageDepotRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Storage depot with code '" + request.code() + "' already exists");
        }

        StorageDepot depot = new StorageDepot(
                request.code().toUpperCase(),
                request.name(),
                request.location(),
                request.capacityMetricTonnes()
        );

        StorageDepot saved = storageDepotRepository.save(depot);
        return StorageDepotResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public StorageDepotResponse getDepotById(Long id) {
        StorageDepot depot = storageDepotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("StorageDepot", "id", id));
        return StorageDepotResponse.fromEntity(depot);
    }

    @Transactional(readOnly = true)
    public List<StorageDepotResponse> getAllActiveDepots() {
        return storageDepotRepository.findByActiveTrue().stream()
                .map(StorageDepotResponse::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public PagedResponse<StorageDepotResponse> getAllDepots(Pageable pageable) {
        Page<StorageDepot> page = storageDepotRepository.findAll(pageable);
        return PagedResponse.from(page.map(StorageDepotResponse::fromEntity));
    }
}
