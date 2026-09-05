package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.ProductCreateRequest;
import com.example.inventoryordersystem.dto.request.ProductUpdateRequest;
import com.example.inventoryordersystem.dto.response.ProductResponse;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.exception.ProductCodeAlreadyExistsException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // 1. 商品一覧取得（ページネーション対応）
    public Page<ProductResponse> getAllProducts(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(ProductResponse::fromEntity);
    }

    // 2. 商品詳細取得
    public ProductResponse getProductById(Long id) {
        Product product = findProductById(id);
        return ProductResponse.fromEntity(product);
    }

    // 3. 商品新規登録
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        // 商品コードの重複チェック
        if (productRepository.existsByProductCode(request.getProductCode())) {
            throw new ProductCodeAlreadyExistsException("商品コードは既に存在します: " + request.getProductCode());
        }

        Product product = Product.builder()
                .productCode(request.getProductCode())
                .name(request.getName())
                .price(request.getPrice())
                .currentStock(request.getCurrentStock())
                .safetyStock(request.getSafetyStock())
                .build();

        Product savedProduct = productRepository.save(product);
        return ProductResponse.fromEntity(savedProduct);
    }

    // 4. 商品情報更新
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = findProductById(id);

        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setCurrentStock(request.getCurrentStock());
        product.setSafetyStock(request.getSafetyStock());

        // @Transactional により自動で変更検知（Dirty Checking）が行われUPDATEされる
        return ProductResponse.fromEntity(product);
    }

    // 5. 商品削除
    @Transactional
    public void deleteProduct(Long id) {
        Product product = findProductById(id);
        productRepository.delete(product);
    }

    // 共通処理: IDで検索し存在しない場合は ResourceNotFoundException をスロー
    private Product findProductById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("指定されたIDの商品が見つかりません: " + id));
    }
}
