package com.zuriagro.service;

import com.zuriagro.domain.entity.Farmer;
import com.zuriagro.domain.entity.FarmerStatus;
import com.zuriagro.domain.entity.User;
import com.zuriagro.dto.common.PagedResponse;
import com.zuriagro.dto.farmer.CreateFarmerRequest;
import com.zuriagro.dto.farmer.FarmerResponse;
import com.zuriagro.dto.farmer.UpdateFarmerRequest;
import com.zuriagro.exception.DuplicateResourceException;
import com.zuriagro.exception.ResourceNotFoundException;
import com.zuriagro.repository.FarmerRepository;
import com.zuriagro.repository.GrainDepositRepository;
import com.zuriagro.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FarmerServiceTest {

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private GrainDepositRepository grainDepositRepository;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private FarmerService farmerService;

    private User staffUser;
    private Farmer sampleFarmer;

    @BeforeEach
    void setUp() {
        staffUser = new User("officer_mary", "mary@zuriagro.com", "hash", "Mary Wanjiru", "0700000001");
        staffUser.setId(10L);

        sampleFarmer = new Farmer(
                "FRM-2026-0001", "John Kamau", "12345678", "0711000003",
                "john@farm.com", "Trans Nzoia", "Kiminini", "Sikhendu", "Scheme Plot 1"
        );
        sampleFarmer.setId(1L);
        sampleFarmer.setRegisteredBy(staffUser);
    }

    @Test
    void createFarmer_Success() {
        CreateFarmerRequest request = new CreateFarmerRequest(
                "John Kamau", "12345678", "0711000003", "john@farm.com",
                "Trans Nzoia", "Kiminini", "Sikhendu", "Scheme Plot 1", null
        );

        when(farmerRepository.existsByPhoneNumber("0711000003")).thenReturn(false);
        when(farmerRepository.existsByNationalId("12345678")).thenReturn(false);
        when(farmerRepository.count()).thenReturn(0L);
        when(farmerRepository.existsByFarmerCode(anyString())).thenReturn(false);
        when(userRepository.findByUsername("officer_mary")).thenReturn(Optional.of(staffUser));
        when(farmerRepository.save(any(Farmer.class))).thenAnswer(invocation -> {
            Farmer f = invocation.getArgument(0);
            f.setId(1L);
            return f;
        });

        FarmerResponse response = farmerService.createFarmer(request, "officer_mary");

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.fullName()).isEqualTo("John Kamau");
        assertThat(response.farmerCode()).startsWith("FRM-");
        assertThat(response.status()).isEqualTo(FarmerStatus.ACTIVE);

        verify(auditLogService).logAction(eq("Farmer"), eq(1L), eq("CREATE"), eq("officer_mary"), anyString());
    }

    @Test
    void createFarmer_DuplicatePhone_ThrowsException() {
        CreateFarmerRequest request = new CreateFarmerRequest(
                "John Kamau", "12345678", "0711000003", null,
                "Trans Nzoia", "Kiminini", null, null, null
        );

        when(farmerRepository.existsByPhoneNumber("0711000003")).thenReturn(true);

        assertThatThrownBy(() -> farmerService.createFarmer(request, "officer_mary"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("phone number");
    }

    @Test
    void createFarmer_DuplicateNationalId_ThrowsException() {
        CreateFarmerRequest request = new CreateFarmerRequest(
                "John Kamau", "12345678", "0711000003", null,
                "Trans Nzoia", "Kiminini", null, null, null
        );

        when(farmerRepository.existsByPhoneNumber("0711000003")).thenReturn(false);
        when(farmerRepository.existsByNationalId("12345678")).thenReturn(true);

        assertThatThrownBy(() -> farmerService.createFarmer(request, "officer_mary"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("National ID");
    }

    @Test
    void updateFarmer_Success() {
        UpdateFarmerRequest request = new UpdateFarmerRequest(
                "John Kamau Updated", "12345678", "0711000003", "john.updated@farm.com",
                "Trans Nzoia", "Kiminini", "Central", "New Plot", FarmerStatus.ACTIVE
        );

        when(farmerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmer));
        when(farmerRepository.save(any(Farmer.class))).thenReturn(sampleFarmer);

        FarmerResponse response = farmerService.updateFarmer(1L, request, "officer_mary");

        assertThat(response).isNotNull();
        assertThat(sampleFarmer.getFullName()).isEqualTo("John Kamau Updated");
        verify(auditLogService).logAction(eq("Farmer"), eq(1L), eq("UPDATE"), eq("officer_mary"), anyString());
    }

    @Test
    void setFarmerStatus_DeactivatesFarmer() {
        when(farmerRepository.findById(1L)).thenReturn(Optional.of(sampleFarmer));
        when(farmerRepository.save(any(Farmer.class))).thenReturn(sampleFarmer);

        FarmerResponse response = farmerService.setFarmerStatus(1L, FarmerStatus.INACTIVE, "officer_mary");

        assertThat(response.status()).isEqualTo(FarmerStatus.INACTIVE);
        verify(auditLogService).logAction(eq("Farmer"), eq(1L), eq("STATUS_CHANGE"), eq("officer_mary"), anyString());
    }

    @Test
    void getFarmers_SearchAndFilter() {
        Pageable pageable = PageRequest.of(0, 10);
        when(farmerRepository.searchFarmers("Kamau", FarmerStatus.ACTIVE, pageable))
                .thenReturn(new PageImpl<>(List.of(sampleFarmer), pageable, 1));

        PagedResponse<FarmerResponse> response = farmerService.getFarmers("Kamau", FarmerStatus.ACTIVE, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.content().get(0).farmerCode()).isEqualTo("FRM-2026-0001");
    }
}
