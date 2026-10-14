package com.zuriagro.dto.farmer;

import com.zuriagro.domain.entity.FarmerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateFarmerRequest(
        @Size(min = 2, max = 150, message = "Full name must be between 2 and 150 characters")
        String fullName,

        @Size(max = 50, message = "National ID must not exceed 50 characters")
        String nationalId,

        @Size(min = 9, max = 50, message = "Phone number must be between 9 and 50 characters")
        String phoneNumber,

        @Email(message = "Invalid email format")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Size(max = 100, message = "County must not exceed 100 characters")
        String county,

        @Size(max = 100, message = "Sub-county must not exceed 100 characters")
        String subCounty,

        @Size(max = 100, message = "Ward must not exceed 100 characters")
        String ward,

        @Size(max = 255, message = "Location must not exceed 255 characters")
        String location,

        FarmerStatus status
) {}
