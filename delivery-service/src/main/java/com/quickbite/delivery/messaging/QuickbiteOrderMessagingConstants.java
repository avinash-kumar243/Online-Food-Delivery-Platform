package com.quickbite.delivery.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_CREATED_QUEUE = "quickbite.delivery-service.order-created";
    public static final String DELIVERY_ASSIGNED_ROUTING_KEY = "delivery.assigned";
    public static final String DELIVERY_PICKED_UP_ROUTING_KEY = "delivery.picked_up";
    public static final String DELIVERY_COMPLETED_ROUTING_KEY = "delivery.completed";

    private QuickbiteOrderMessagingConstants() {
    }
}
