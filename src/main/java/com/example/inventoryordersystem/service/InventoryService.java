package com.example.inventoryordersystem.service;

import com.example.inventoryordersystem.dto.request.InventoryAdjustmentRequest;
import com.example.inventoryordersystem.dto.response.InventoryAlertResponse;
import com.example.inventoryordersystem.dto.response.InventoryStatusResponse;
import com.example.inventoryordersystem.dto.response.StockHistoryResponse;
import com.example.inventoryordersystem.entity.Product;
import com.example.inventoryordersystem.entity.StockHistory;
import com.example.inventoryordersystem.entity.User;
import com.example.inventoryordersystem.exception.BusinessException;
import com.example.inventoryordersystem.exception.ResourceNotFoundException;
import com.example.inventoryordersystem.repository.ProductRepository;
import com.example.inventoryordersystem.repository.StockHistoryRepository;
import com.example.inventoryordersystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockHistoryRepository stockHistoryRepository;
    private final UserRepository userRepository;

    public Page<InventoryStatusResponse> getInventoryList(Pageable pageable) {
        return productRepository.findAll(pageable)
                .map(InventoryStatusResponse::fromEntity);
    }

    public List<InventoryAlertResponse> getInventoryAlerts() {
        return productRepository.findLowStockProducts().stream()
                .map(InventoryAlertResponse::fromEntity)
                .toList();
    }

    @Transactional
    public StockHistoryResponse adjustStock(InventoryAdjustmentRequest request, String currentUsername) {
        if (request.quantityChange() == 0) {
            throw new BusinessException("在庫変動数には 0 以外の値を指定してください");
        }

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new ResourceNotFoundException("指定された商品が存在しません ID: " + request.productId()));

        User user = userRepository.findByEmail(currentUsername)
                .orElseThrow(() -> new ResourceNotFoundException("ログインユーザーが見つかりません: " + currentUsername));

        int currentStock = product.getCurrentStock() != null ? product.getCurrentStock() : 0;
        int updatedStock = currentStock + request.quantityChange();
        if (updatedStock < 0) {
            throw new BusinessException(
                    "商品名: " + product.getName() + " の在庫が不足しているため調整できません。（現在庫: " + currentStock + "）"
            );
        }

        product.setCurrentStock(updatedStock);

        StockHistory history = StockHistory.builder()
                .product(product)
                .user(user)
                .changeType(request.changeType())
                .quantityChange(request.quantityChange())
                .reason(request.reason())
                .build();

        StockHistory savedHistory = stockHistoryRepository.save(history);
        return StockHistoryResponse.fromEntity(savedHistory, updatedStock);
    }
}