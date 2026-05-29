package com.quickbite.restaurant.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String RESTAURANT_APPROVED_ROUTING_KEY = "restaurant.approved";
    public static final String RESTAURANT_REJECTED_ROUTING_KEY = "restaurant.rejected";
    public static final String RESTAURANT_STATUS_CHANGED_ROUTING_KEY = "restaurant.status.changed";
    public static final String RESTAURANT_DELETED_ROUTING_KEY = "restaurant.deleted";

    private QuickbiteOrderMessagingConstants() {
    }
}
