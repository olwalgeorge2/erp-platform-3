# Commerce POS Service

## 1. Purpose
1.1 Operate in-store and mobile point-of-sale experiences with offline resilience.
1.2 Manage checkout, payments, receipts, and cash management for physical locations.
1.3 Synchronize in-store orders with central commerce, inventory, and financial systems.

## 2. Module Structure
2.1 `pos-application/` - Runs POS workflows, device commands, and synchronization processes.
2.2 `pos-domain/` - Models tills, shifts, payment transactions, and offline order queues.
2.3 `pos-infrastructure/` - Integrates with hardware devices, payment terminals, and store systems.

## 3. Domain Highlights
3.1 Supports multi-register operations, shift handoffs, and offline order capture with replay.
3.2 Aligns tender types, taxation, and reconciliation with Financial Management policies.
3.3 Utilizes shared catalog and pricing logic from `commerce-shared` modules.

## 4. Integration
4.1 Streams orders and payments to central Commerce services and Financial Management.
4.2 Pulls inventory availability and price updates from Inventory and Commerce shared services.
4.3 Emits telemetry for device health and store performance to Business Intelligence.
