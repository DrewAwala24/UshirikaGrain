package com.zuriagro.service;

import com.zuriagro.domain.entity.GrainType;
import com.zuriagro.dto.graintype.CreateGrainTypeRequest;
import com.zuriagro.dto.graintype.GrainTypeResponse;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.repository.GrainTypeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GrainTypeService {

    private final GrainTypeRepository grainTypeRepository;

    public GrainTypeService(GrainTypeRepository grainTypeRepository) {
        this.grainTypeRepository = grainTypeRepository;
    }

    @Transactional
    public GrainTypeResponse createGrainType(CreateGrainTypeRequest request) {
        if (grainTypeRepository.existsByCode(request.code())) {
            throw new DuplicateResourceException("Grain type with code '" + request.code() + "' already exists");
        }

        GrainType grainType = new GrainType(
                request.code().toUpperCase(),
                request.name(),
                request.description(),
                request.unitOfMeasure() != null ? request.unitOfMeasure().toUpperCase() : "KG"
        );

        GrainType saved = grainTypeRepository.save(grainType);
        return GrainTypeResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public GrainTypeResponse getGrainTypeById(Long id) {
        GrainType grainType = grainTypeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GrainType", "id", id));
        return GrainTypeResponse.fromEntity(grainType);
    }

    @Transactional(readOnly = true)
    public List<GrainTypeResponse> getAllActiveGrainTypes() {
        return grainTypeRepository.findByActiveTrue().stream()
                .map(GrainTypeResponse::fromEntity)
                .toList();
    }
}
