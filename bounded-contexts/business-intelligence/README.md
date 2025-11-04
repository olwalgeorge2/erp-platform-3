# Business Intelligence Context

## 1. Purpose
1.1 Deliver analytics, reporting, and data visualization capabilities for the ERP platform.
1.2 Consolidate operational data into analytical projections that drive decision-making and KPIs.
1.3 Provide governed access to insights with strict tenant isolation and role-based controls.

## 2. Module Overview
2.1 `bi-application/` - Orchestrates analytics queries, dashboard assembly, and reporting workflows.
2.2 `bi-domain/` - Defines analytical aggregates, value objects, and modeling policies.
2.3 `bi-infrastructure/` - Connects to the data warehouse, manages ETL adapters, and publishes datasets.

## 3. Integration Highlights
3.1 Consumes domain events from core operational contexts (finance, inventory, procurement) via the platform event bus.
3.2 Exposes RESTful read models and scheduled exports through the API gateway.
3.3 Emits telemetry on query performance and dashboard adoption to the observability mesh.

## 4. Reference
4.1 See `docs/ARCHITECTURE.md` (Business Intelligence) for domain narratives and diagrams.
4.2 `docs/ROADMAP.md` Phase 5 tracks the rollout schedule for analytics features.
4.3 `bounded-contexts/README.md` provides a catalog view across all contexts.
