# Accounts Receivable Service

## 1. Purpose
1.1 Manage customer invoicing, collections, cash application, and credit exposure.
1.2 Support billing across commerce channels, subscriptions, and service engagements.
1.3 Provide visibility into receivables aging, disputes, and revenue forecasts.

## 2. Module Structure
2.1 `ar-application/` - Controls billing schedules, collections workflows, and credit management commands.
2.2 `ar-domain/` - Models invoices, payments, disputes, and receivable aging structures.
2.3 `ar-infrastructure/` - Integrates with payment processors, bank reconciliation, and dunning tools.

## 3. Domain Highlights
3.1 Aligns revenue recognition policies with Financial Accounting aggregates.
3.2 Coordinates credit checks and holds with Commerce and Customer Relation contexts.
3.3 Supports multi-tenant invoicing templates, currencies, and tax handling.

## 4. Integration
4.1 Consumes order completion events from Commerce, Operations, and Manufacturing services.
4.2 Publishes cash application and receivable status events to Financial Accounting and Business Intelligence.
4.3 Sends customer statements and reminders through Communication Hub.
