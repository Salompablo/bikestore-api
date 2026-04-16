package com.bikestore.api.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "shipping_zones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShippingZone {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "zip_prefix", nullable = false)
    private String zipPrefix;

    @Column(nullable = false)
    private BigDecimal cost;

    @Column(name = "estimated_days", nullable = false)
    private Integer estimatedDays;

    @Column(nullable = false)
    @Builder.Default
    private String provider = "Envío gestionado por Bikes Asaro";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ShippingZone that = (ShippingZone) o;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
