package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.InventoryAdjustmentRequest;
import com.example.inventoryordersystem.dto.response.InventoryAlertResponse;
import com.example.inventoryordersystem.dto.response.InventoryStatusResponse;
import com.example.inventoryordersystem.dto.response.StockHistoryResponse;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.entity.Role;
import com.example.inventoryordersystem.entity.StockChangeType;
import com.example.inventoryordersystem.entity.StockHistory;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.exception.BusinessException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.ProductRepository;
import com.example.inventoryordersystem.repository.StockHistoryRepository;
import com.example.inventoryordersystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StockHistoryRepository stockHistoryRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    @DisplayName("リアルタイム在庫一覧取得：ページネーションで現在庫・安全在庫・アラート判定が返却されること")
    void getInventoryList_Success() {
        Product lowStockProduct = Product.builder()
                .id(1L)
                .productCode("PROD-LOW")
                .name("低在庫商品")
                .price(new BigDecimal("1000.00"))
                .currentStock(3)
                .safetyStock(10)
                .version(0)
                .build();
        Product normalProduct = Product.builder()
                .id(2L)
                .productCode("PROD-OK")
                .name("通常在庫商品")
                .price(new BigDecimal("2000.00"))
                .currentStock(20)
                .safetyStock(10)
                .version(1)
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        given(productRepository.findAll(pageable))
                .willReturn(new PageImpl<>(List.of(lowStockProduct, normalProduct), pageable, 2));

        Page<InventoryStatusResponse> result = inventoryService.getInventoryList(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent().get(0).belowSafetyStock()).isTrue();
        assertThat(result.getContent().get(0).currentStock()).isEqualTo(3);
        assertThat(result.getContent().get(1).belowSafetyStock()).isFalse();
        assertThat(result.getContent().get(1).currentStock()).isEqualTo(20);
    }

    @Test
    @DisplayName("安全在庫アラート取得：安全在庫を下回る商品と不足数が算出されること")
    void getInventoryAlerts_Success() {
        Product alertProduct = Product.builder()
                .id(1L)
                .productCode("PROD-ALERT")
                .name("アラート対象商品")
                .price(new BigDecimal("3000.00"))
                .currentStock(4)
                .safetyStock(10)
                .version(0)
                .build();

        given(productRepository.findLowStockProducts()).willReturn(List.of(alertProduct));

        List<InventoryAlertResponse> alerts = inventoryService.getInventoryAlerts();

        assertThat(alerts).hasSize(1);
        assertThat(alerts.getFirst().productCode()).isEqualTo("PROD-ALERT");
        assertThat(alerts.getFirst().currentStock()).isEqualTo(4);
        assertThat(alerts.getFirst().safetyStock()).isEqualTo(10);
        assertThat(alerts.getFirst().shortageQuantity()).isEqualTo(6);
    }

    @Test
    @DisplayName("手動在庫調整：正常に入出庫数と履歴が更新・保存されること")
    void adjustStock_Success() {
        Product product = Product.builder()
                .id(1L)
                .productCode("PROD-001")
                .name("テスト商品")
                .price(new BigDecimal("5000.00"))
                .currentStock(10)
                .safetyStock(5)
                .version(0)
                .build();
        User user = User.builder()
                .id(1L)
                .email("staff@example.com")
                .passwordHash("hashed")
                .name("担当者")
                .role(Role.STAFF)
                .build();

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                1L,
                StockChangeType.ADJUSTMENT,
                5,
                "棚卸差異調整"
        );

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(userRepository.findByEmail("staff@example.com")).willReturn(Optional.of(user));
        given(stockHistoryRepository.save(any(StockHistory.class))).willAnswer(invocation -> {
            StockHistory h = invocation.getArgument(0);
            return StockHistory.builder()
                    .id(100L)
                    .product(h.getProduct())
                    .user(h.getUser())
                    .changeType(h.getChangeType())
                    .quantityChange(h.getQuantityChange())
                    .reason(h.getReason())
                    .createdAt(LocalDateTime.now())
                    .build();
        });

        StockHistoryResponse response = inventoryService.adjustStock(request, "staff@example.com");

        assertThat(product.getCurrentStock()).isEqualTo(15);
        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.changeType()).isEqualTo(StockChangeType.ADJUSTMENT);
        assertThat(response.quantityChange()).isEqualTo(5);
        assertThat(response.currentStockAfter()).isEqualTo(15);
        assertThat(response.reason()).isEqualTo("棚卸差異調整");
        verify(stockHistoryRepository).save(any(StockHistory.class));
    }

    @Test
    @DisplayName("手動在庫調整：調整後の在庫がマイナスになる場合は BusinessException が発生すること")
    void adjustStock_NegativeStock_ThrowsBusinessException() {
        Product product = Product.builder()
                .id(1L)
                .productCode("PROD-001")
                .name("テスト商品")
                .price(new BigDecimal("5000.00"))
                .currentStock(3)
                .safetyStock(5)
                .version(0)
                .build();
        User user = User.builder()
                .id(1L)
                .email("staff@example.com")
                .passwordHash("hashed")
                .name("担当者")
                .role(Role.STAFF)
                .build();

        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                1L,
                StockChangeType.OUTBOUND,
                -5,
                "出荷数過多"
        );

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(userRepository.findByEmail("staff@example.com")).willReturn(Optional.of(user));

        assertThatThrownBy(() -> inventoryService.adjustStock(request, "staff@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("在庫が不足しているため調整できません");
        assertThat(product.getCurrentStock()).isEqualTo(3);
    }

    @Test
    @DisplayName("手動在庫調整：変動数が 0 の場合は BusinessException が発生すること")
    void adjustStock_ZeroQuantity_ThrowsBusinessException() {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                1L,
                StockChangeType.ADJUSTMENT,
                0,
                "変動なし"
        );

        assertThatThrownBy(() -> inventoryService.adjustStock(request, "staff@example.com"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("0 以外の値を指定してください");
    }

    @Test
    @DisplayName("手動在庫調整：存在しない商品IDの場合は ResourceNotFoundException が発生すること")
    void adjustStock_ProductNotFound_ThrowsResourceNotFoundException() {
        InventoryAdjustmentRequest request = new InventoryAdjustmentRequest(
                999L,
                StockChangeType.INBOUND,
                10,
                "入荷"
        );

        given(productRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inventoryService.adjustStock(request, "staff@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("指定された商品が存在しません");
    }
}