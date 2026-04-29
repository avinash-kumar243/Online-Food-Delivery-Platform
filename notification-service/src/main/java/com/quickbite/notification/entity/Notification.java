package com.quickbite.notification.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
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
    @Max(value = Integer.MAX_VALUE, message = "recipientId is invalid")
    @Column(nullable = false)
    private Integer recipientId;

    @Column(nullable = false)
    private LocalDateTime sentAt;

    @NotBlank(message = "type is required")
    @Pattern(regexp = "ORDER|PAYMENT|PROMO|DELIVERY", message = "type must be ORDER, PAYMENT, PROMO, or DELIVERY")
    @Column(nullable = false, length = 20)
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

    @Column(nullable = false)
    private boolean isRead;

    @PrePersist
    public void prePersist() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}
