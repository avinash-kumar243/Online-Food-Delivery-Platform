package com.quickbite.payment.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_CREATED_QUEUE = "quickbite.payment-service.order-created";
    public static final String ORDER_DELIVERED_QUEUE = "quickbite.payment-service.order-delivered";

    private QuickbiteOrderMessagingConstants() {
    }
}
