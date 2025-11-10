package com.erp.apigateway

import jakarta.ws.rs.core.Application

/**
 * API Gateway Application Entry Point.
 *
 * Health checks are now provided by SmallRye Health:
 * - GET /q/health/live - Liveness probe (gateway process health)
 * - GET /q/health/ready - Readiness probe (Redis + backend services)
 * - GET /q/health - Combined health status
 */
class ApiGatewayApplication : Application()
