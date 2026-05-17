package com.quickbite.review.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_DELIVERED_QUEUE = "quickbite.review-service.order-delivered";
    public static final String NOTIFICATION_EXCHANGE = "quickbite.notification.exchange";

    private QuickbiteOrderMessagingConstants() {
    }
}
