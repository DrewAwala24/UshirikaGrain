package com.zuriagro.dto.graintype;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateGrainTypeRequest(
        @NotBlank(message = "Grain code is required")
        @Size(max = 50, message = "Code must not exceed 50 characters")
        String code,

        @NotBlank(message = "Grain name is required")
        @Size(max = 100, message = "Name must not exceed 100 characters")
        String name,

        @Size(max = 255, message = "Description must not exceed 255 characters")
        String description,

        @NotBlank(message = "Unit of measure is required")
        @Size(max = 20, message = "Unit of measure must not exceed 20 characters")
        String unitOfMeasure
) {}
