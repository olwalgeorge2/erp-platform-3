# Manufacturing Production Service

## 1. Purpose
1.1 Plan, schedule, and execute production orders across plants and lines.
1.2 Track routing, materials consumption, and production performance in real time.
1.3 Support make-to-stock, make-to-order, and configure-to-order manufacturing strategies.

## 2. Module Structure
2.1 `production-application/` - Coordinates scheduling, dispatching, and progress reporting workflows.
2.2 `production-domain/` - Models production orders, routings, work centers, and BOM consumption.
2.3 `production-infrastructure/` - Integrates with MES, IoT signals, and shop-floor automation.

## 3. Domain Highlights
3.1 Calculates capacity and constraint-based scheduling leveraging Inventory and Procurement data.
3.2 Tracks yield, scrap, and OEE metrics for continuous improvement.
3.3 Generates cost captures and WIP updates for Financial Management.

## 4. Integration
4.1 Consumes demand signals from Commerce and supply plans from Procurement.
4.2 Publishes production completion and material usage events to Inventory and Financial contexts.
4.3 Shares performance telemetry with Business Intelligence and Operations Service.
