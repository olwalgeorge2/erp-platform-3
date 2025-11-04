# Accounts Payable Service

## 1. Purpose
1.1 Manage supplier invoices, approvals, payments, and payables accounting.
1.2 Enforce payment terms, discount capture, and compliance with procurement policies.
1.3 Provide visibility into liabilities, cash requirements, and vendor performance.

## 2. Module Structure
2.1 `ap-application/` - Orchestrates invoice processing, approval workflows, and payment runs.
2.2 `ap-domain/` - Models invoices, vouchers, payment schedules, and hold statuses.
2.3 `ap-infrastructure/` - Integrates with banking, payment gateways, and document capture systems.

## 3. Domain Highlights
3.1 Validates invoices against purchase orders and receipts from Procurement modules.
3.2 Applies segregation-of-duties and approval thresholds defined in Tenancy & Identity policies.
3.3 Generates accruals and payable postings for the Financial Accounting service.

## 4. Integration
4.1 Consumes purchase order and receiving events from Procurement to facilitate matching.
4.2 Publishes payment and liability events to Financial Accounting and Business Intelligence.
4.3 Sends supplier notifications via Communication Hub for remittance advice and exceptions.
