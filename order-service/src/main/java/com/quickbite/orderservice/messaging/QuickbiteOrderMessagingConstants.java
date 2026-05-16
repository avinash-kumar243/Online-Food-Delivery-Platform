package com.quickbite.orderservice.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String PAYMENT_SUCCESS_QUEUE = "quickbite.order-service.payment-success";
    public static final String RESTAURANT_ACCEPTED_QUEUE = "quickbite.order-service.restaurant-accepted";
    public static final String DELIVERY_ASSIGNED_QUEUE = "quickbite.order-service.delivery-assigned";
    public static final String ORDER_PICKED_UP_QUEUE = "quickbite.order-service.order-pickedup";
    public static final String ORDER_DELIVERED_QUEUE = "quickbite.order-service.order-delivered";

    private QuickbiteOrderMessagingConstants() {
    }
}
