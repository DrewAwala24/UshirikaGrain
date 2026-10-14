package com.zuriagro.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "farmers")
@EntityListeners(AuditingEntityListener.class)
public class Farmer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "farmer_code", nullable = false, unique = true, length = 50)
    private String farmerCode;

    @Column(name = "full_name", nullable = false, length = 150)
    private String fullName;

    @Column(name = "national_id", unique = true, length = 50)
    private String nationalId;

    @Column(name = "phone_number", nullable = false, length = 50)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "county", length = 100)
    private String county;

    @Column(name = "sub_county", length = 100)
    private String subCounty;

    @Column(name = "ward", length = 100)
    private String ward;

    @Column(name = "location", length = 255)
    private String location;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private FarmerStatus status = FarmerStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by_user_id")
    private User registeredBy;

    @OneToMany(mappedBy = "farmerRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<GrainDeposit> deposits = new ArrayList<>();

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public Farmer() {}

    public Farmer(String farmerCode, String fullName, String nationalId, String phoneNumber,
                  String email, String county, String subCounty, String ward, String location) {
        this.farmerCode = farmerCode;
        this.fullName = fullName;
        this.nationalId = nationalId;
        this.phoneNumber = phoneNumber;
        this.email = email;
        this.county = county;
        this.subCounty = subCounty;
        this.ward = ward;
        this.location = location;
        this.status = FarmerStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getFarmerCode() { return farmerCode; }
    public void setFarmerCode(String farmerCode) { this.farmerCode = farmerCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getNationalId() { return nationalId; }
    public void setNationalId(String nationalId) { this.nationalId = nationalId; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getCounty() { return county; }
    public void setCounty(String county) { this.county = county; }

    public String getSubCounty() { return subCounty; }
    public void setSubCounty(String subCounty) { this.subCounty = subCounty; }

    public String getWard() { return ward; }
    public void setWard(String ward) { this.ward = ward; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public FarmerStatus getStatus() { return status; }
    public void setStatus(FarmerStatus status) { this.status = status; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public User getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(User registeredBy) { this.registeredBy = registeredBy; }

    public List<GrainDeposit> getDeposits() { return deposits; }
    public void setDeposits(List<GrainDeposit> deposits) { this.deposits = deposits; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Farmer farmer = (Farmer) o;
        return Objects.equals(id, farmer.id) || Objects.equals(farmerCode, farmer.farmerCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, farmerCode);
    }
}
