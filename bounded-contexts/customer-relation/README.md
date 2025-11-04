# Customer Relation Context

## 1. Purpose
1.1 Manage the customer lifecycle from marketing campaigns through CRM and service support.
1.2 Consolidate customer profiles, engagements, and service entitlements for a unified experience.
1.3 Coordinate customer-facing processes with Commerce, Communication Hub, and Operations Service.

## 2. Module Overview
2.1 `customer-campaigns/` - Campaign planning, segmentation, and outbound journey orchestration.
2.2 `customer-crm/` - Account hierarchy, interaction history, and opportunity tracking.
2.3 `customer-support/` - Case intake, SLA management, and ticket escalations.
2.4 `customer-shared/` - Shared customer domain objects, preferences, and policy helpers.

## 3. Integration Highlights
3.1 Synchronizes customer master data with Tenancy & Identity for authentication and access.
3.2 Feeds engagement triggers to Communication Hub and receives notification outcomes.
3.3 Exchanges order and fulfillment context with Commerce and Operations Service for closed-loop support.

## 4. Reference
4.1 `docs/ARCHITECTURE.md` (Customer Relation) covers collaboration diagrams and data views.
4.2 Delivery sequencing is outlined in `docs/ROADMAP.md` Phase 5.
4.3 Use `bounded-contexts/README.md` for quick cross-context lookup.
