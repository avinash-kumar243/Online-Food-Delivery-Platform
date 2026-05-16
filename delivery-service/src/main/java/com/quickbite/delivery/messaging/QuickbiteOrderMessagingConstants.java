package com.quickbite.delivery.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String RESTAURANT_ACCEPTED_QUEUE = "quickbite.delivery-service.restaurant-accepted";
    public static final String ORDER_DELIVERED_QUEUE = "quickbite.delivery-service.order-delivered";

    private QuickbiteOrderMessagingConstants() {
    }
}
