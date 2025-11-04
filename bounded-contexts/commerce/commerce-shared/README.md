# Commerce Shared Kernel

## 1. Purpose
1.1 Provide shared domain components that underpin all Commerce channel services.
1.2 Maintain canonical definitions for catalog items, pricing policies, and order lifecycles.
1.3 Ensure consistency and reusability across B2B, eCommerce, Marketplace, and POS modules.

## 2. Module Overview
2.1 `catalog-shared/` - Shared catalog entities, attributes, and classification helpers.
2.2 `order-shared/` - Canonical order aggregate, fulfillment states, and event payload definitions.
2.3 `pricing-shared/` - Pricing rules, discount frameworks, tax models, and rounding policies.

## 3. Usage Guidelines
3.1 Export interfaces and value objects as versioned APIs to minimize coupling.
3.2 Prohibit channel-specific logic; keep modules focused on reusable primitives and policies.
3.3 Coordinate schema evolution with downstream consumers via ADRs and release notes.

## 4. Integration
4.1 Shared modules are consumed by Commerce channel services and select external contexts (Financial, Inventory).
4.2 Publish domain events through platform eventing abstractions defined in `platform-infrastructure/`.
4.3 Maintain contract tests ensuring compatibility with each consuming module.
