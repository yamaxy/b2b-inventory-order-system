package com.example.inventoryordersystem.repository;

import com.example.inventoryordersystem.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 商品コードによる重複チェック
    boolean existsByProductCode(String productCode);

    // 商品コードによる検索
    Optional<Product> findByProductCode(String productCode);

    @Query("SELECT p FROM Product p WHERE p.currentStock < p.safetyStock ORDER BY p.id ASC")
    List<Product> findLowStockProducts();
}