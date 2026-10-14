package com.zuriagro.service;

import com.zuriagro.domain.entity.*;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GrainDepositServiceTest {

    @Mock
    private GrainDepositRepository grainDepositRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private StorageDepotRepository storageDepotRepository;

    @Mock
    private GrainTypeRepository grainTypeRepository;

    @Mock
    private InventoryService inventoryService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private GrainDepositService grainDepositService;

    private User farmerUser;
    private User adminUser;
    private StorageDepot depot;
    private GrainType grainType;
    private GrainDeposit pendingDeposit;

    @BeforeEach
    void setUp() {
        Role farmerRole = new Role(RoleType.ROLE_FARMER, "Farmer");
        Role adminRole = new Role(RoleType.ROLE_ADMIN, "Admin");

        farmerUser = new User("farmer_john", "farmer@test.com", "hash", "John Kamau", "0711");
        farmerUser.setId(10L);
        farmerUser.setRoles(Set.of(farmerRole));

        adminUser = new User("admin_mary", "admin@test.com", "hash", "Mary Wanjiku", "0722");
        adminUser.setId(20L);
        adminUser.setRoles(Set.of(adminRole));

        depot = new StorageDepot("ELD-01", "Eldoret Silo", "Eldoret", new BigDecimal("50000.00"));
        depot.setId(1L);
        depot.setActive(true);

        grainType = new GrainType("MAIZE", "White Maize", "Grade 1 Maize", "KG");
        grainType.setId(1L);
        grainType.setActive(true);

        pendingDeposit = new GrainDeposit();
        pendingDeposit.setId(100L);
        pendingDeposit.setDepositReference("DEP-TEST-001");
        pendingDeposit.setFarmer(farmerUser);
        pendingDeposit.setDepot(depot);
        pendingDeposit.setGrainType(grainType);
        pendingDeposit.setQuantity(new BigDecimal("5000.00"));
        pendingDeposit.setUnitOfMeasure("KG");
        pendingDeposit.setStatus(DepositStatus.PENDING);
        pendingDeposit.setCreatedBy(farmerUser);
    }

    private void authenticateAs(String username, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                username, null, List.of(new SimpleGrantedAuthority(role))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    void createDeposit_Success_AsFarmer() {
        authenticateAs("farmer_john", "ROLE_FARMER");

        CreateGrainDepositRequest request = new CreateGrainDepositRequest(
                10L, 1L, 1L, new BigDecimal("2500.00"), "KG", "Harvest deposit"
        );

        when(userRepository.findByUsername("farmer_john")).thenReturn(Optional.of(farmerUser));
        when(storageDepotRepository.findById(1L)).thenReturn(Optional.of(depot));
        when(grainTypeRepository.findById(1L)).thenReturn(Optional.of(grainType));
        when(grainDepositRepository.save(any(GrainDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GrainDepositResponse response = grainDepositService.createDeposit(request, "farmer_john");

        assertThat(response).isNotNull();
        assertThat(response.status()).isEqualTo(DepositStatus.PENDING);
        assertThat(response.quantity()).isEqualByComparingTo(new BigDecimal("2500.00"));
        assertThat(response.farmerUsername()).isEqualTo("farmer_john");
        assertThat(response.depotCode()).isEqualTo("ELD-01");

        verify(grainDepositRepository).save(any(GrainDeposit.class));
        verify(auditLogService).logAction(eq("GrainDeposit"), any(), eq("CREATE"), eq("farmer_john"), anyString());
    }

    @Test
    void createDeposit_ThrowsUnauthorized_WhenFarmerCreatesForAnotherUser() {
        authenticateAs("farmer_john", "ROLE_FARMER");

        CreateGrainDepositRequest request = new CreateGrainDepositRequest(
                999L, 1L, 1L, new BigDecimal("2500.00"), "KG", null
        );

        when(userRepository.findByUsername("farmer_john")).thenReturn(Optional.of(farmerUser));

        assertThatThrownBy(() -> grainDepositService.createDeposit(request, "farmer_john"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Farmers can only create deposit records for themselves");

        verify(grainDepositRepository, never()).save(any());
    }

    @Test
    void createDeposit_ThrowsInvalidOperation_WhenDepotInactive() {
        authenticateAs("farmer_john", "ROLE_FARMER");
        depot.setActive(false);

        CreateGrainDepositRequest request = new CreateGrainDepositRequest(
                null, 1L, 1L, new BigDecimal("1000.00"), "KG", null
        );

        when(userRepository.findByUsername("farmer_john")).thenReturn(Optional.of(farmerUser));
        when(storageDepotRepository.findById(1L)).thenReturn(Optional.of(depot));

        assertThatThrownBy(() -> grainDepositService.createDeposit(request, "farmer_john"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Cannot record deposit for an inactive storage depot");
    }

    @Test
    void acceptDeposit_Success_AtomicallyCreditsInventory() {
        authenticateAs("admin_mary", "ROLE_ADMIN");

        when(grainDepositRepository.findById(100L)).thenReturn(Optional.of(pendingDeposit));
        when(userRepository.findByUsername("admin_mary")).thenReturn(Optional.of(adminUser));
        when(grainDepositRepository.save(any(GrainDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepositDecisionRequest request = new DepositDecisionRequest(null, "Grain quality inspected and approved");
        GrainDepositResponse response = grainDepositService.acceptDeposit(100L, request, "admin_mary");

        assertThat(response.status()).isEqualTo(DepositStatus.ACCEPTED);
        assertThat(response.reviewedByUsername()).isEqualTo("admin_mary");

        // Verify that stock update and audit were called
        verify(inventoryService).recordDepositAcceptance(pendingDeposit, adminUser);
        verify(grainDepositRepository).save(pendingDeposit);
        verify(auditLogService).logAction(eq("GrainDeposit"), eq(100L), eq("ACCEPT"), eq("admin_mary"), anyString());
    }

    @Test
    void acceptDeposit_ThrowsInvalidOperation_WhenAlreadyAccepted() {
        authenticateAs("admin_mary", "ROLE_ADMIN");
        pendingDeposit.setStatus(DepositStatus.ACCEPTED);

        when(grainDepositRepository.findById(100L)).thenReturn(Optional.of(pendingDeposit));

        DepositDecisionRequest request = new DepositDecisionRequest(null, "Attempt repeat acceptance");

        assertThatThrownBy(() -> grainDepositService.acceptDeposit(100L, request, "admin_mary"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("cannot be accepted because current status is ACCEPTED");

        verify(inventoryService, never()).recordDepositAcceptance(any(), any());
    }

    @Test
    void rejectDeposit_Success_SetsRejectionReasonWithoutStockUpdate() {
        authenticateAs("admin_mary", "ROLE_ADMIN");

        when(grainDepositRepository.findById(100L)).thenReturn(Optional.of(pendingDeposit));
        when(userRepository.findByUsername("admin_mary")).thenReturn(Optional.of(adminUser));
        when(grainDepositRepository.save(any(GrainDeposit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DepositDecisionRequest request = new DepositDecisionRequest("Moisture content exceeded threshold (17.5%)", null);
        GrainDepositResponse response = grainDepositService.rejectDeposit(100L, request, "admin_mary");

        assertThat(response.status()).isEqualTo(DepositStatus.REJECTED);
        assertThat(response.rejectionReason()).isEqualTo("Moisture content exceeded threshold (17.5%)");
        assertThat(response.reviewedByUsername()).isEqualTo("admin_mary");

        // Verify inventory was NOT updated
        verify(inventoryService, never()).recordDepositAcceptance(any(), any());
        verify(grainDepositRepository).save(pendingDeposit);
        verify(auditLogService).logAction(eq("GrainDeposit"), eq(100L), eq("REJECT"), eq("admin_mary"), anyString());
    }

    @Test
    void rejectDeposit_ThrowsException_WhenReasonMissing() {
        authenticateAs("admin_mary", "ROLE_ADMIN");

        when(grainDepositRepository.findById(100L)).thenReturn(Optional.of(pendingDeposit));

        DepositDecisionRequest request = new DepositDecisionRequest("", null);

        assertThatThrownBy(() -> grainDepositService.rejectDeposit(100L, request, "admin_mary"))
                .isInstanceOf(InvalidOperationException.class)
                .hasMessageContaining("Rejection reason is mandatory");
    }

    @Test
    void getDepositById_ThrowsUnauthorized_WhenFarmerAccessesOtherFarmerDeposit() {
        authenticateAs("farmer_other", "ROLE_FARMER");

        when(grainDepositRepository.findById(100L)).thenReturn(Optional.of(pendingDeposit));

        assertThatThrownBy(() -> grainDepositService.getDepositById(100L, "farmer_other"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not authorized to access this deposit record");
    }
}
