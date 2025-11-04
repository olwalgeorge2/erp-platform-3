# Customer CRM Service

## 1. Purpose
1.1 Maintain customer accounts, contacts, hierarchies, and engagement history.
1.2 Support opportunity management, pipeline forecasting, and relationship tracking.
1.3 Provide a 360-degree view of customer interactions across contexts.

## 2. Module Structure
2.1 `crm-application/` - Coordinates CRM workflows, opportunity management, and insights delivery.
2.2 `crm-domain/` - Defines customer aggregates, account hierarchies, and interaction records.
2.3 `crm-infrastructure/` - Integrates with external CRM tools, data enrichment, and analytics services.

## 3. Domain Highlights
3.1 Synchronizes with Tenancy & Identity to align user roles and access controls.
3.2 Shares account context with Commerce, Support, and Operations services for consistent experiences.
3.3 Maintains activity timelines and notes for compliance and service continuity.

## 4. Integration
4.1 Emits customer lifecycle events to Communication Hub and Business Intelligence.
4.2 Consumes order, case, and service updates from connected contexts to enrich profiles.
4.3 Publishes forecast and pipeline metrics to Financial Management and executive dashboards.
