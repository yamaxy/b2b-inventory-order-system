package com.example.inventoryordersystem.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 商品コードは原則変更不可のため Setter なし
    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    private String productCode;

    @Setter
    @Column(nullable = false, length = 200)
    private String name;

    @Setter
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Setter
    @Column(name = "current_stock", nullable = false)
    @Builder.Default
    private Integer currentStock = 0;

    @Setter
    @Column(name = "safety_stock", nullable = false)
    @Builder.Default
    private Integer safetyStock = 0;

    // JPAが楽観的ロックで自動制御するため Setter 不要
    @Version
    @Column(nullable = false)
    @Builder.Default
    private Integer version = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
