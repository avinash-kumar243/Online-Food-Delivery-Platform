package com.quickbite.auth.messaging;

public final class QuickbiteNotificationMessagingConstants {

    public static final String NOTIFICATION_EXCHANGE = "quickbite.notification.exchange";
    public static final String NOTIFICATION_DLX = "quickbite.notification.dlx.exchange";
    public static final String PASSWORD_RESET_OTP_ROUTING_KEY = "auth.password-reset-otp";
    public static final String USER_CREATED_ROUTING_KEY = "auth.user.created";
    public static final String USER_SUSPENDED_ROUTING_KEY = "auth.user.suspended";
    public static final String USER_REACTIVATED_ROUTING_KEY = "auth.user.reactivated";
    public static final String USER_DELETED_ROUTING_KEY = "auth.user.deleted";

    private QuickbiteNotificationMessagingConstants() {
    }
}
