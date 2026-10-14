package com.zuriagro.integration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zuriagro.domain.entity.*;
import com.zuriagro.dto.deposit.CreateGrainDepositRequest;
import com.zuriagro.dto.deposit.DepositDecisionRequest;
import com.zuriagro.repository.*;
import com.zuriagro.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class GrainDepositWorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StorageDepotRepository storageDepotRepository;

    @Autowired
    private GrainTypeRepository grainTypeRepository;

    @Autowired
    private GrainDepositRepository grainDepositRepository;

    @Autowired
    private InventoryBalanceRepository inventoryBalanceRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private User farmer;
    private User admin;
    private StorageDepot depot;
    private GrainType grainType;
    private String farmerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        inventoryMovementRepository.deleteAll();
        inventoryBalanceRepository.deleteAll();
        grainDepositRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();
        storageDepotRepository.deleteAll();
        grainTypeRepository.deleteAll();

        Role farmerRole = roleRepository.save(new Role(RoleType.ROLE_FARMER, "Farmer"));
        Role adminRole = roleRepository.save(new Role(RoleType.ROLE_ADMIN, "Admin"));

        farmer = new User("farmer_simon", "simon@farm.com", passwordEncoder.encode("Pass@123"), "Simon Njoroge", "0712345678");
        farmer.setRoles(Set.of(farmerRole));
        farmer = userRepository.save(farmer);

        admin = new User("admin_ann", "ann@zuriagro.com", passwordEncoder.encode("Pass@123"), "Ann Mwangi", "0723456789");
        admin.setRoles(Set.of(adminRole));
        admin = userRepository.save(admin);

        depot = storageDepotRepository.save(new StorageDepot("NAK-01", "Nakuru Central Silo", "Nakuru", new BigDecimal("30000.00")));
        grainType = grainTypeRepository.save(new GrainType("WHEAT", "Hard Red Winter Wheat", "Bread wheat", "KG"));

        farmerToken = jwtTokenProvider.generateTokenFromUsername("farmer_simon");
        adminToken = jwtTokenProvider.generateTokenFromUsername("admin_ann");
    }

    @Test
    void testCompleteVerticalSlice_DepositLifecycleAndStockUpdate() throws Exception {
        // Step 1: Farmer submits a grain deposit
        CreateGrainDepositRequest createRequest = new CreateGrainDepositRequest(
                farmer.getId(),
                depot.getId(),
                grainType.getId(),
                new BigDecimal("1500.00"),
                "KG",
                "Season 2026 harvest"
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/deposits")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.quantity").value(1500.00))
                .andExpect(jsonPath("$.depotCode").value("NAK-01"))
                .andExpect(jsonPath("$.grainTypeCode").value("WHEAT"))
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long depositId = createdJson.get("id").asLong();
        String depositReference = createdJson.get("depositReference").asText();

        // Step 2: Critical Rule Check - Inventory stock must NOT increase on deposit creation
        mockMvc.perform(get("/api/v1/depots/" + depot.getId() + "/inventory")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Step 3: Farmer retrieves the deposit by ID
        mockMvc.perform(get("/api/v1/deposits/" + depositId)
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.depositReference").value(depositReference))
                .andExpect(jsonPath("$.status").value("PENDING"));

        // Step 4: Authorization Check - Farmer cannot accept the deposit (Requires ADMIN/DEPOT_MANAGER)
        mockMvc.perform(patch("/api/v1/deposits/" + depositId + "/accept")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositDecisionRequest(null, "Attempt unauthorized acceptance"))))
                .andExpect(status().isForbidden());

        // Step 5: Admin reviews and accepts the deposit
        DepositDecisionRequest acceptRequest = new DepositDecisionRequest(null, "Grain inspected: Grade 1 accepted");
        mockMvc.perform(patch("/api/v1/deposits/" + depositId + "/accept")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(acceptRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.reviewedByUsername").value("admin_ann"));

        // Step 6: Verify Atomic Inventory Balance Update
        mockMvc.perform(get("/api/v1/inventory/depots/" + depot.getId() + "/grain-types/" + grainType.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity").value(1500.00))
                .andExpect(jsonPath("$.availableQuantity").value(1500.00));

        // Step 7: Verify Audit Movement Ledger was populated
        mockMvc.perform(get("/api/v1/inventory/movements")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("depotId", String.valueOf(depot.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].movementType").value("DEPOSIT_ACCEPTANCE"))
                .andExpect(jsonPath("$.content[0].quantity").value(1500.00))
                .andExpect(jsonPath("$.content[0].previousBalance").value(0.00))
                .andExpect(jsonPath("$.content[0].newBalance").value(1500.00))
                .andExpect(jsonPath("$.content[0].sourceDepositReference").value(depositReference));

        // Step 8: Idempotency & Concurrency Check - Attempting to re-accept an already accepted deposit must fail
        mockMvc.perform(patch("/api/v1/deposits/" + depositId + "/accept")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new DepositDecisionRequest(null, "Second acceptance"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Operation"));

        // Verify balance was NOT duplicated
        InventoryBalance finalBalance = inventoryBalanceRepository
                .findByDepotIdAndGrainTypeId(depot.getId(), grainType.getId())
                .orElseThrow();
        assertThat(finalBalance.getQuantity()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }
}
