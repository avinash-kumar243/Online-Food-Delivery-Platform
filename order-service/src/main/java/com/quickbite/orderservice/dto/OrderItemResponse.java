package com.quickbite.orderservice.dto;

import java.math.BigDecimal;

public record OrderItemResponse(
    Long orderItemId,
    Long menuItemId,
    String name,
    BigDecimal price,
    Integer quantity,
    String customization,
    BigDecimal lineTotal
) {
}
