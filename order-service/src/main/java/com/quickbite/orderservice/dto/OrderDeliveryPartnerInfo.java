package com.quickbite.orderservice.dto;

public record OrderDeliveryPartnerInfo(
    Long deliveryAgentId,
    Long userId,
    String fullName,
    String phone,
    String verificationStatus
) {
}
