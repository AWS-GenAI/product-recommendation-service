package com.example.recommendation.client;

import com.example.recommendation.config.RecommendationProperties;
import com.example.recommendation.exception.RecommendationServiceException;
import com.example.recommendation.model.ProductRecommendation;
import com.example.recommendation.model.RecommendationRequest;
import com.example.recommendation.model.RecommendationResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Client for interacting with the recommendation service API.
 * Implements the IRecommendationService interface.
 * 
 * This class handles authentication, request formatting, response parsing, and error handling.
 * 
 * @see IRecommendationService
 */
@Component
public class RecommendationClient implements IRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationClient.class);
    private static final int MAX_RETRIES = 3;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final RecommendationProperties properties;

    public RecommendationClient(WebClient.Builder webClientBuilder, 
                               ObjectMapper objectMapper,
                               RecommendationProperties properties) {
        this.webClient = webClientBuilder
                .baseUrl(properties.getApiUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-Key", properties.getApiKey())
                .build();
        this.objectMapper = objectMapper;
        this.properties = properties;
        
        logger.info("Initialized RecommendationClient with API URL: {}", properties.getApiUrl());
    }

    /**
     * Retrieves personalized recommendations for a given user ID.
     *
     * @param userId The user ID to get recommendations for
     * @param limit Maximum number of recommendations to return
     * @return List of product recommendations
     * @throws RecommendationServiceException if the API call fails
     */
    @Override
    @Retryable(
        value = {RecommendationServiceException.class},
        maxAttempts = MAX_RETRIES,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ProductRecommendation> getUserRecommendations(String userId, int limit) {
        logger.debug("Getting user recommendations for userId: {}, limit: {}", userId, limit);
        
        RecommendationRequest request = new RecommendationRequest();
        request.setUserId(userId);
        request.setLimit(limit);
        
        try {
            return webClient.post()
                .uri("/recommendations/user")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RecommendationResponse.class)
                .map(RecommendationResponse::getRecommendations)
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(1))
                    .filter(this::isRetryableException)
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying getUserRecommendations after exception: {}", 
                            retrySignal.failure().getMessage())))
                .doOnError(this::logError)
                .onErrorResume(this::handleError)
                .block();
        } catch (Exception e) {
            logger.error("Failed to get user recommendations", e);
            throw new RecommendationServiceException("Failed to get user recommendations", e);
        }
    }

    /**
     * Retrieves product-based recommendations for a given product ID.
     *
     * @param productId The product ID to get recommendations for
     * @param limit Maximum number of recommendations to return
     * @return List of product recommendations
     * @throws RecommendationServiceException if the API call fails
     */
    @Override
    @Retryable(
        value = {RecommendationServiceException.class},
        maxAttempts = MAX_RETRIES,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public List<ProductRecommendation> getProductRecommendations(String productId, int limit) {
        logger.debug("Getting product recommendations for productId: {}, limit: {}", productId, limit);
        
        RecommendationRequest request = new RecommendationRequest();
        request.setProductId(productId);
        request.setLimit(limit);
        
        try {
            return webClient.post()
                .uri("/recommendations/product")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RecommendationResponse.class)
                .map(RecommendationResponse::getRecommendations)
                .retryWhen(Retry.backoff(MAX_RETRIES, Duration.ofSeconds(1))
                    .filter(this::isRetryableException)
                    .doBeforeRetry(retrySignal -> 
                        logger.warn("Retrying getProductRecommendations after exception: {}", 
                            retrySignal.failure().getMessage())))
                .doOnError(this::logError)
                .onErrorResume(this::handleError)
                .block();
        } catch (Exception e) {
            logger.error("Failed to get product recommendations", e);
            throw new RecommendationServiceException("Failed to get product recommendations", e);
        }
    }

    /**
     * Determines if an exception is retryable.
     *
     * @param throwable The exception to check
     * @return true if the exception is retryable, false otherwise
     */
    private boolean isRetryableException(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) throwable;
            int statusCode = wcre.getStatusCode().value();
            // Retry on server errors and some specific client errors
            return statusCode >= 500 || statusCode == 429;
        }
        return true; // Retry on other exceptions like connection issues
    }

    /**
     * Logs an error from the API call.
     *
     * @param throwable The exception that occurred
     */
    private void logError(Throwable throwable) {
        if (throwable instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) throwable;
            logger.error("API error: {} - {}", wcre.getStatusCode(), wcre.getResponseBodyAsString());
        } else {
            logger.error("API call failed", throwable);
        }
    }

    /**
     * Handles an error from the API call by returning an empty list of recommendations.
     *
     * @param throwable The exception that occurred
     * @return An empty list of recommendations
     */
    private Mono<List<ProductRecommendation>> handleError(Throwable throwable) {
        logger.warn("Returning empty recommendations due to error: {}", throwable.getMessage());
        return Mono.just(Collections.emptyList());
    }

    /**
     * Parses the API response.
     *
     * @param responseBody The response body as a string
     * @return The parsed response
     * @throws JsonProcessingException if the response cannot be parsed
     */
    private RecommendationResponse parseResponse(String responseBody) throws JsonProcessingException {
        try {
            return objectMapper.readValue(responseBody, RecommendationResponse.class);
        } catch (JsonProcessingException e) {
            logger.error("Failed to parse API response: {}", responseBody, e);
            throw e;
        }
    }
}
