package com.quickbite.notification.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer notificationId;

    @Min(value = 1, message = "recipientId must be greater than 0")
    @Column(nullable = false)
    private Long recipientId;

    @Pattern(
        regexp = "CUSTOMER|RESTAURANT_OWNER|DELIVERY_PARTNER|ADMIN",
        message = "recipientRole must be CUSTOMER, RESTAURANT_OWNER, DELIVERY_PARTNER, or ADMIN"
    )
    @Column(length = 32)
    private String recipientRole;

    @Column(nullable = false)
    private Instant sentAt;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "[A-Z_]+", message = "type must contain uppercase letters and underscores only")
    @Column(nullable = false, length = 64)
    private String type;

    @NotBlank(message = "channel is required")
    @Pattern(regexp = "APP|EMAIL|SMS", message = "channel must be APP, EMAIL, or SMS")
    @Column(nullable = false, length = 20)
    private String channel;

    @NotBlank(message = "title is required")
    @Size(max = 150, message = "title must not exceed 150 characters")
    @Column(nullable = false, length = 150)
    private String title;

    @NotBlank(message = "message is required")
    @Size(max = 2000, message = "message must not exceed 2000 characters")
    @Column(nullable = false, length = 2000)
    private String message;

    @Size(max = 100, message = "relatedId must not exceed 100 characters")
    @Column(length = 100)
    private String relatedId;

    @Size(max = 100, message = "relatedType must not exceed 100 characters")
    @Column(length = 100)
    private String relatedType;

    @Size(max = 40, message = "orderId must not exceed 40 characters")
    @Column(length = 40)
    private String orderId;

    @Size(max = 40, message = "deliveryId must not exceed 40 characters")
    @Column(length = 40)
    private String deliveryId;

    private Integer rating;

    @Size(max = 160, message = "actorName must not exceed 160 characters")
    @Column(length = 160)
    private String actorName;

    @Size(max = 2000, message = "reviewText must not exceed 2000 characters")
    @Column(length = 2000)
    private String reviewText;

    @Column(nullable = false)
    private boolean isRead;

    private Instant readAt;

    @PrePersist
    public void prePersist() {
        if (sentAt == null) {
            sentAt = Instant.now();
        }
    }

    public String getNotificationType() {
        return type;
    }
}
