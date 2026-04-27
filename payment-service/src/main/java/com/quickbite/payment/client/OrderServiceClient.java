package com.quickbite.payment.client;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Component
public class OrderServiceClient {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceClient.class);

    private final RestTemplate restTemplate;
    private final String orderServiceBaseUrl;

    public OrderServiceClient(RestTemplate restTemplate,
                              @Value("${quickbite.order-service.base-url:http://ORDER-SERVICE}") String orderServiceBaseUrl) {
        this.restTemplate = restTemplate;
        this.orderServiceBaseUrl = orderServiceBaseUrl;
    }

    public void updateOrderPaymentStatus(Long orderId, String paymentStatus) {
        try {
            // TODO: Align this URL and request payload with the final order-service payment status contract.
            String url = orderServiceBaseUrl + "/api/v1/orders/" + orderId + "/payment-status";
            HttpEntity<Map<String, String>> requestEntity = new HttpEntity<>(Map.of("paymentStatus", paymentStatus));
            restTemplate.exchange(url, HttpMethod.PUT, requestEntity, Void.class);
        } catch (RestClientException exception) {
            log.warn("Could not update order payment status for orderId={}: {}", orderId, exception.getMessage());
        }
    }
}
