package com.example.inventoryordersystem.repository;

import com.example.inventoryordersystem.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@SuppressWarnings("unused")
public interface ProductRepository extends JpaRepository<Product, Long> {

    // 商品コードによる重複チェック
    boolean existsByProductCode(String productCode);

    // 商品コードによる検索
    Optional<Product> findByProductCode(String productCode); // 後で利用する
}

