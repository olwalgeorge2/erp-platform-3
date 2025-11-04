# Customer Campaigns Service

## 1. Purpose
1.1 Orchestrate marketing campaigns, customer journeys, and segmentation strategies.
1.2 Manage campaign assets, scheduling, audience selection, and performance tracking.
1.3 Align marketing operations with consent rules and multi-tenant governance.

## 2. Module Structure
2.1 `campaigns-application/` - Drives campaign workflows, journey orchestration, and real-time triggers.
2.2 `campaigns-domain/` - Models audiences, segments, campaign entities, and performance metrics.
2.3 `campaigns-infrastructure/` - Interfaces with communication channels, asset repositories, and data platforms.

## 3. Domain Highlights
3.1 Utilizes consent and preference data from `customer-shared` modules and Tenancy & Identity.
3.2 Tracks attribution and outcome metrics for Business Intelligence pipelines.
3.3 Supports automated and manual orchestration while enforcing throttling policies.

## 4. Integration
4.1 Publishes campaign events to Communication Hub for message delivery.
4.2 Consumes customer profile updates and interactions from CRM and Support services.
4.3 Shares ROI and engagement data with Financial Management and Business Intelligence.
