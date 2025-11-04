# Operations Service Context

## 1. Purpose
1.1 Coordinate field service operations, dispatching, and resource scheduling for service teams.
1.2 Provide technicians with task visibility, asset history, and parts requirements on the go.
1.3 Ensure customer commitments, SLAs, and service outcomes are tracked end-to-end.

## 2. Module Overview
2.1 `operations-field-service/` - Work order management, dispatching, and mobile technician workflows.
2.2 `operations-shared/` - Shared operations domain components, enumerations, and policy helpers.

## 3. Integration Highlights
3.1 Consumes cases from Customer Relation and work requests from Manufacturing or Procurement.
3.2 Updates Inventory for parts usage and Financial Management for service billing and time capture.
3.3 Generates communication triggers for Communication Hub to notify customers and technicians.

## 4. Reference
4.1 Refer to `docs/ARCHITECTURE.md` (Operations Service) for service flow diagrams.
4.2 Roadmap sequencing appears in `docs/ROADMAP.md` Phase 5.
4.3 Cross-context alignment notes live in `bounded-contexts/README.md`.
