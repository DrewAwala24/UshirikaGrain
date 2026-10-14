package com.zuriagro.dto.deposit;

import jakarta.validation.constraints.Size;

public record DepositDecisionRequest(
        @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
        String rejectionReason,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes
) {}
