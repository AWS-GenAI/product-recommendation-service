# Product Recommendation Service

This repository contains the standalone product recommendation service for the e-commerce platform.

## Project Overview

This service implements the product recommendation engine described in [ECOM-1001](../jira_stories/ECOM-1001.json) and uses the recommendation service client from [ECOM-1002](../jira_stories/ECOM-1002.json).

## Features

- Personalized product recommendations based on user browsing history
- Similar product recommendations based on product attributes
- Frequently bought together recommendations
- Popular in category recommendations
- Recently viewed products tracking
- A/B testing framework for recommendation algorithms
- Performance monitoring and analytics

## Architecture

The recommendation service uses a hybrid approach:
1. Collaborative filtering for personalized recommendations
2. Content-based filtering for similar products
3. Association rule mining for frequently bought together
4. Popularity-based recommendations as fallback

## Technology Stack

- Java 17
- Spring Boot 3.x
- MongoDB for storing user behavior and recommendation models
- Redis for caching recommendations
- Apache Kafka for real-time event processing
- Apache Spark ML for offline model training
- Docker for containerization
- AWS infrastructure (ECS, S3, CloudWatch)

## API Endpoints

### User Recommendations
```
GET /api/v1/recommendations/user/{userId}
```

### Product Recommendations
```
GET /api/v1/recommendations/product/{productId}
```

### Category Recommendations
```
GET /api/v1/recommendations/category/{categoryId}
```

### Recently Viewed
```
GET /api/v1/recommendations/recently-viewed/{userId}
```

## Getting Started

### Prerequisites
- JDK 17
- Maven 3.8+
- Docker and Docker Compose
- MongoDB
- Redis

### Build and Run
```bash
# Build the service
mvn clean package

# Run with Docker Compose
docker-compose up
```

## Performance Requirements

- Response time: < 200ms for recommendation retrieval
- Throughput: 1000+ requests per second
- Availability: 99.99% uptime
- Cache hit ratio: > 90%

## Monitoring and Analytics

The service includes:
- Prometheus metrics for performance monitoring
- Custom analytics for recommendation effectiveness
- A/B testing framework for algorithm comparison
- Dashboards for visualizing recommendation performance

## Related Documentation

- [E-Commerce Platform Overview](../confluence_samples/01_ecommerce_platform_overview.html)
- [Product Requirements Document](../confluence_samples/07_product_requirements_document.html)
