# Commerce Context

## 1. Purpose
1.1 Enable multi-channel selling for B2B portals, e-commerce storefronts, marketplaces, and points of sale.
1.2 Harmonize pricing, catalog, and order lifecycle rules across sales channels while preserving channel-specific logic.
1.3 Coordinate promotions, taxes, and customer entitlements with downstream fulfillment and financial services.

## 2. Module Overview
2.1 `commerce-b2b/` - Enterprise purchasing journeys, contract terms, approval flows.
2.2 `commerce-ecommerce/` - Direct-to-consumer storefront, carts, and checkout experiences.
2.3 `commerce-marketplace/` - Third-party marketplace integrations and listing syndication.
2.4 `commerce-pos/` - In-store point-of-sale operations and offline resiliency.
2.5 `commerce-shared/` - Shared catalog, pricing, and order abstractions reused by channel services.

## 3. Integration Highlights
3.1 Interfaces with Customer Relation for account hierarchies, segmentation, and support cases.
3.2 Publishes order events to Inventory Management, Procurement, and Financial Management for fulfillment and billing.
3.3 Coordinates promotions, tax calculation, and payment authorization through shared platform services.

## 4. Reference
4.1 Review `docs/ARCHITECTURE.md` (Commerce) for relationships and context collaborations.
4.2 Implementation milestones are detailed in `docs/ROADMAP.md` Phase 5.
4.3 Additional shared patterns live in `platform-infrastructure/` and `platform-shared/` modules.
