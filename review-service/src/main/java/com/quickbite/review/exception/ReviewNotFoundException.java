package com.quickbite.review.exception;

public class ReviewNotFoundException extends ResourceNotFoundException {

    public ReviewNotFoundException(Long reviewId) {
        super("Review not found with id: " + reviewId);
    }
}
