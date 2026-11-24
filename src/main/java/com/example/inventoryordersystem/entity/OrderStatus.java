package com.example.inventoryordersystem.entity;

import java.util.EnumSet;
import java.util.Set;

public enum OrderStatus {
    PENDING,
    PREPARING,
    COMPLETED,
    CANCELLED;

    /**
     * 現在のステータスから遷移可能なステータス一覧を返却します。
     */
    public Set<OrderStatus> allowedTransitions() {
        return switch (this) {
            case PENDING -> EnumSet.of(PREPARING, CANCELLED);
            case PREPARING -> EnumSet.of(COMPLETED, CANCELLED);
            case COMPLETED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    /**
     * 指定された次ステータスへの遷移が許可されているかを判定します。
     */
    public boolean canTransitionTo(OrderStatus nextStatus) {
        if (nextStatus == null || this == nextStatus) {
            return false;
        }
        return allowedTransitions().contains(nextStatus);
    }

    /**
     * 不正な遷移の場合に例外をスローし、遷移を検証します。
     */
    public void validateTransitionTo(OrderStatus nextStatus) {
        if (!canTransitionTo(nextStatus)) {
            throw new IllegalStateException(
                    String.format("受注ステータスを %s から %s へ遷移させることはできません。", this.name(), nextStatus)
            );
        }
    }
}