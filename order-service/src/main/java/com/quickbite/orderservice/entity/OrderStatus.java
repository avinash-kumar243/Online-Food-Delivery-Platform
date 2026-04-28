package com.quickbite.orderservice.entity;

public enum OrderStatus {
    PLACED,
    CONFIRMED,
    PREPARING,
    READY_FOR_PICKUP,
    PICKED_UP,
    OUT_FOR_DELIVERY,
    DELIVERED,
    CANCELLED
}
