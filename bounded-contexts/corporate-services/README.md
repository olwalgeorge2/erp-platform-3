# Corporate Services Context

## 1. Purpose
1.1 Support internal corporate operations covering asset lifecycle management and human resources.
1.2 Provide shared corporate policies, reference data, and compliance controls for back-office teams.
1.3 Integrate with financial, procurement, and operations workflows for end-to-end governance.

## 2. Module Overview
2.1 `corporate-assets/` - Fixed asset registry, tracking, depreciation, and maintenance coordination.
2.2 `corporate-hr/` - Employee records, onboarding, compensation, and workforce planning.
2.3 `corporate-shared/` - Common corporate domain primitives, enumerations, and policy enforcement helpers.

## 3. Integration Highlights
3.1 Syncs personnel and asset data with Financial Management for capitalization and payroll.
3.2 Exposes HR and asset services to Operations Service for field assignments and parts usage.
3.3 Consumes identity and tenancy events to provision roles and entitlements.

## 4. Reference
4.1 Consult `docs/ARCHITECTURE.md` (Corporate Services) for detailed domain diagrams.
4.2 Roadmap prioritization appears in `docs/ROADMAP.md` Phase 5.
4.3 Shared HR compliance requirements are tracked via ADRs in `docs/adr/`.
