package com.quickbite.restaurant.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_CREATED_QUEUE = "quickbite.restaurant-service.order-created";
    public static final String PAYMENT_SUCCESS_QUEUE = "quickbite.restaurant-service.payment-success";
    public static final String DELIVERY_ASSIGNED_QUEUE = "quickbite.restaurant-service.delivery-assigned";

    private QuickbiteOrderMessagingConstants() {
    }
}
