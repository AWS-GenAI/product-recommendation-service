package com.example.recommendation.client;

import com.example.recommendation.model.ProductRecommendation;
import java.util.List;

/**
 * Interface for recommendation service operations.
 * Defines the contract for retrieving product recommendations.
 */
public interface IRecommendationService {
    
    /**
     * Retrieves personalized recommendations for a given user ID.
     *
     * @param userId The user ID to get recommendations for
     * @param limit Maximum number of recommendations to return
     * @return List of product recommendations
     */
    List<ProductRecommendation> getUserRecommendations(String userId, int limit);
    
    /**
     * Retrieves product-based recommendations for a given product ID.
     *
     * @param productId The product ID to get recommendations for
     * @param limit Maximum number of recommendations to return
     * @return List of product recommendations
     */
    List<ProductRecommendation> getProductRecommendations(String productId, int limit);
}
