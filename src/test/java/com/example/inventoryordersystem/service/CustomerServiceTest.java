package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.CustomerCreateRequest;
import com.example.inventoryordersystem.dto.response.CustomerResponse;
import com.example.inventoryordersystem.entity.Customer;
import com.example.inventoryordersystem.exception.BusinessException;
import com.example.inventoryordersystem.repository.CustomerRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    @Test
    @DisplayName("取引先新規作成: 正常系")
    void createCustomer_Success() {
        // given
        CustomerCreateRequest request = new CustomerCreateRequest(
                "CUST-100",
                "テスト顧客",
                "03-1234-5678",
                "東京都千代田区"
        );

        given(customerRepository.existsByCustomerCode("CUST-100")).willReturn(false);

        Customer savedCustomer = Customer.builder()
                .id(1L)
                .customerCode("CUST-100")
                .name("テスト顧客")
                .phoneNumber("03-1234-5678")
                .address("東京都千代田区")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(customerRepository.save(any(Customer.class))).willReturn(savedCustomer);

        // when
        CustomerResponse response = customerService.createCustomer(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.customerCode()).isEqualTo("CUST-100");
        assertThat(response.name()).isEqualTo("テスト顧客");
        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    @DisplayName("取引先新規作成: 取引先コード重複による例外発生")
    void createCustomer_DuplicateCode_ThrowsException() {
        // given
        CustomerCreateRequest request = new CustomerCreateRequest(
                "CUST-100",
                "テスト顧客",
                "03-1234-5678",
                "東京都千代田区"
        );

        given(customerRepository.existsByCustomerCode("CUST-100")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> customerService.createCustomer(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("取引先コードが既に存在します");
    }
}