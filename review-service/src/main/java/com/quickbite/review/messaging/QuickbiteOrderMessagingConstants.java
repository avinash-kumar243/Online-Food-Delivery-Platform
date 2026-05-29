package com.quickbite.review.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_COMPLETED_QUEUE = "quickbite.review-service.order-completed";
    public static final String DELIVERY_COMPLETED_QUEUE = "quickbite.review-service.delivery-completed";
    public static final String REVIEW_CREATED_ROUTING_KEY = "review.created";

    private QuickbiteOrderMessagingConstants() {
    }
}
