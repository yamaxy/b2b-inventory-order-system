package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.ProductCreateRequest;
import com.example.inventoryordersystem.dto.request.ProductUpdateRequest;
import com.example.inventoryordersystem.dto.response.ProductResponse;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.exception.ProductCodeAlreadyExistsException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    @DisplayName("商品新規作成: 正常系")
    void createProduct_Success() {
        // given (テスト用リクエストデータの設定)
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode("PROD-001");
        request.setName("テスト商品");
        request.setPrice(new BigDecimal("1000"));
        request.setCurrentStock(100);
        request.setSafetyStock(10);

        given(productRepository.existsByProductCode("PROD-001")).willReturn(false);

        Product savedProduct = Product.builder()
                .id(1L)
                .productCode("PROD-001")
                .name("テスト商品")
                .price(new BigDecimal("1000"))
                .currentStock(100)
                .safetyStock(10)
                .build();

        given(productRepository.save(any(Product.class))).willReturn(savedProduct);

        // when (Serviceメソッドの実行)
        ProductResponse response = productService.createProduct(request);

        // then (検証)
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getProductCode()).isEqualTo("PROD-001");
        assertThat(response.getName()).isEqualTo("テスト商品");
        verify(productRepository).save(any(Product.class));
    }

    @Test
    @DisplayName("商品新規作成: 商品コード重複時に例外が発生すること")
    void createProduct_DuplicateCode_ThrowsException() {
        // given
        ProductCreateRequest request = new ProductCreateRequest();
        request.setProductCode("PROD-001");

        given(productRepository.existsByProductCode("PROD-001")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> productService.createProduct(request))
                .isInstanceOf(ProductCodeAlreadyExistsException.class)
                .hasMessageContaining("商品コードは既に存在します: PROD-001");
    }

    @Test
    @DisplayName("商品情報更新: 正常系 (ProductUpdateRequest を使用)")
    void updateProduct_Success() {
        // given
        Long productId = 1L;
        Product existingProduct = Product.builder()
                .id(productId)
                .productCode("PROD-001")
                .name("旧商品名")
                .price(new BigDecimal("1000"))
                .currentStock(50)
                .safetyStock(5)
                .build();

        ProductUpdateRequest updateRequest = new ProductUpdateRequest();
        updateRequest.setName("新商品名");
        updateRequest.setPrice(new BigDecimal("1500"));
        updateRequest.setCurrentStock(80);
        updateRequest.setSafetyStock(10);

        given(productRepository.findById(productId)).willReturn(Optional.of(existingProduct));

        // when
        ProductResponse response = productService.updateProduct(productId, updateRequest);

        // then
        assertThat(response.getName()).isEqualTo("新商品名");
        assertThat(response.getPrice()).isEqualByComparingTo("1500");
        assertThat(response.getCurrentStock()).isEqualTo(80);
    }

    @Test
    @DisplayName("商品情報更新: 存在しないID指定時に例外が発生すること")
    void updateProduct_NotFound_ThrowsException() {
        // given
        Long nonExistentId = 99L;
        ProductUpdateRequest updateRequest = new ProductUpdateRequest();

        given(productRepository.findById(nonExistentId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> productService.updateProduct(nonExistentId, updateRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("指定されたIDの商品が見つかりません: 99");
    }
}