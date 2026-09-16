package com.example.inventoryordersystem.repository;

import com.example.inventoryordersystem.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    boolean existsByCustomerCode(String customerCode);
    Optional<Customer> findByCustomerCode(String customerCode);
    Page<Customer> findByNameContaining(String name, Pageable pageable);
}

