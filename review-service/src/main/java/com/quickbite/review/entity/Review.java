package com.quickbite.review.entity;

import java.time.LocalDateTime;

import com.quickbite.review.enums.ReviewType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
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
@Table(name = "order_reviews", uniqueConstraints = {
    @UniqueConstraint(name = "uk_order_reviews_order_customer_type", columnNames = {"order_id", "customer_id", "review_type"})
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @NotNull(message = "orderId is required")
    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @NotNull(message = "customerId is required")
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotNull(message = "restaurantId is required")
    @Column(name = "restaurant_id", nullable = false)
    private Long restaurantId;

    @NotNull(message = "agentId is required")
    @Column(name = "agent_id", nullable = false)
    private Long agentId;

    @NotNull(message = "reviewType is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 20)
    private ReviewType reviewType;

    @Min(value = 1, message = "rating must be between 1 and 5")
    @Max(value = 5, message = "rating must be between 1 and 5")
    @Column(nullable = false)
    private int rating;

    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    @Column(length = 1000)
    private String comment;

    @Column(name = "review_date", nullable = false)
    private LocalDateTime reviewDate;

    @Column(name = "is_verified", nullable = false)
    private boolean verified;

    @PrePersist
    public void prePersist() {
        if (reviewDate == null) {
            reviewDate = LocalDateTime.now();
        }
    }
}
