package com.quickbite.payment.messaging;

public final class QuickbiteOrderMessagingConstants {

    public static final String ORDER_EXCHANGE = "quickbite.order.exchange";
    public static final String ORDER_DLX = "quickbite.order.dlx.exchange";
    public static final String ORDER_CREATED_QUEUE = "quickbite.payment-service.order-created";
    public static final String PAYMENT_COMPLETED_ROUTING_KEY = "payment.completed";
    public static final String PAYMENT_FAILED_ROUTING_KEY = "payment.failed";
    public static final String REFUND_INITIATED_ROUTING_KEY = "payment.refund.initiated";

    private QuickbiteOrderMessagingConstants() {
    }
}
