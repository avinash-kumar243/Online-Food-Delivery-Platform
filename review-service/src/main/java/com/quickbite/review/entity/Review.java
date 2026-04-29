package com.quickbite.review.entity;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_order_id", columnNames = "order_id")
})
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reviewId;

    @NotNull(message = "orderId is required")
    @Column(name = "order_id", nullable = false, unique = true)
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

    @Min(value = 1, message = "foodRating must be between 1 and 5")
    @Max(value = 5, message = "foodRating must be between 1 and 5")
    @Column(name = "food_rating", nullable = false)
    private int foodRating;

    @Min(value = 1, message = "deliveryRating must be between 1 and 5")
    @Max(value = 5, message = "deliveryRating must be between 1 and 5")
    @Column(name = "delivery_rating", nullable = false)
    private int deliveryRating;

    @Size(max = 1000, message = "comment must not exceed 1000 characters")
    @Column(length = 1000)
    private String comment;

    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified;

    @PrePersist
    public void prePersist() {
        if (reviewDate == null) {
            reviewDate = LocalDate.now();
        }
    }
}
