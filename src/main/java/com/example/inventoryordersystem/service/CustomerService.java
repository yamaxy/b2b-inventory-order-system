package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.CustomerCreateRequest;
import com.example.inventoryordersystem.dto.request.CustomerUpdateRequest;
import com.example.inventoryordersystem.dto.response.CustomerResponse;
import com.example.inventoryordersystem.entity.Customer;
import com.example.inventoryordersystem.exception.BusinessException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Page<CustomerResponse> getCustomers(String name, Pageable pageable) {
        Page<Customer> customers;
        if (name != null && !name.isBlank()) {
            customers = customerRepository.findByNameContaining(name, pageable);
        } else {
            customers = customerRepository.findAll(pageable);
        }
        return customers.map(CustomerResponse::fromEntity);
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("指定された取引先が存在しません ID: " + id));
        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        if (customerRepository.existsByCustomerCode(request.customerCode())) {
            throw new BusinessException("取引先コードが既に存在します: " + request.customerCode());
        }

        Customer customer = Customer.builder()
                .customerCode(request.customerCode())
                .name(request.name())
                .phoneNumber(request.phoneNumber())
                .address(request.address())
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        return CustomerResponse.fromEntity(savedCustomer);
    }

    @Transactional
    public CustomerResponse updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("指定された取引先が存在しません ID: " + id));

        customer.setName(request.name());
        customer.setPhoneNumber(request.phoneNumber());
        customer.setAddress(request.address());

        return CustomerResponse.fromEntity(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        if (!customerRepository.existsById(id)) {
            throw new ResourceNotFoundException("指定された取引先が存在しません ID: " + id);
        }
        customerRepository.deleteById(id);
    }
}
