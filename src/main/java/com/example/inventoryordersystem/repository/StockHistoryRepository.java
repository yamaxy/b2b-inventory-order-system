package com.example.inventoryordersystem.repository;

import com.example.inventoryordersystem.entity.StockHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    Page<StockHistory> findByProductId(Long productId, Pageable pageable);
}
