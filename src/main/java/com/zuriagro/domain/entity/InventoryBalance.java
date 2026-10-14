package com.zuriagro.domain.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(
    name = "inventory_balances",
    uniqueConstraints = @UniqueConstraint(name = "uq_depot_grain", columnNames = {"depot_id", "grain_type_id"})
)
@EntityListeners(AuditingEntityListener.class)
public class InventoryBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "depot_id", nullable = false)
    private StorageDepot depot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "grain_type_id", nullable = false)
    private GrainType grainType;

    @Column(name = "quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal quantity = BigDecimal.ZERO;

    @Column(name = "reserved_quantity", nullable = false, precision = 15, scale = 2)
    private BigDecimal reservedQuantity = BigDecimal.ZERO;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @LastModifiedDate
    @Column(name = "last_updated", nullable = false)
    private Instant lastUpdated = Instant.now();

    public InventoryBalance() {}

    public InventoryBalance(StorageDepot depot, GrainType grainType, BigDecimal initialQuantity) {
        this.depot = depot;
        this.grainType = grainType;
        this.quantity = initialQuantity != null ? initialQuantity : BigDecimal.ZERO;
        this.reservedQuantity = BigDecimal.ZERO;
    }

    public BigDecimal getAvailableQuantity() {
        return this.quantity.subtract(this.reservedQuantity);
    }

    public void addQuantity(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to add must be positive");
        }
        this.quantity = this.quantity.add(amount);
    }

    public void subtractQuantity(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity to subtract must be positive");
        }
        BigDecimal newQuantity = this.quantity.subtract(amount);
        if (newQuantity.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("Insufficient stock: balance cannot become negative");
        }
        this.quantity = newQuantity;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public StorageDepot getDepot() {
        return depot;
    }

    public void setDepot(StorageDepot depot) {
        this.depot = depot;
    }

    public GrainType getGrainType() {
        return grainType;
    }

    public void setGrainType(GrainType grainType) {
        this.grainType = grainType;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getReservedQuantity() {
        return reservedQuantity;
    }

    public void setReservedQuantity(BigDecimal reservedQuantity) {
        this.reservedQuantity = reservedQuantity;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Instant lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InventoryBalance that)) return false;
        return Objects.equals(depot, that.depot) && Objects.equals(grainType, that.grainType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(depot, grainType);
    }
}
