package com.quickbite.orderservice.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_CONFIRMED_ROUTING_KEY = "order.confirmed";
    public static final String ORDER_COMPLETED_ROUTING_KEY = "order.completed";
    public static final String ORDER_CANCELLED_ROUTING_KEY = "order.cancelled";

    private QuickbiteOrderMessagingConstants() {
    }
}
