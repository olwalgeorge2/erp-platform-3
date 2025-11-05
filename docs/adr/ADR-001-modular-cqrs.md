# ADR-001: Modular CQRS Implementation

**Status**: Accepted  
**Date**: 2025-11-05  
**Deciders**: Architecture Team  
**Tags**: cqrs, architecture, platform-infrastructure

## Context

The ERP platform requires handling complex business workflows spanning multiple bounded contexts. We need to decide how to implement the Command Query Responsibility Segregation (CQRS) pattern across our microservices architecture while maintaining consistency, testability, and developer ergonomics.

## Decision

We will implement a **modular CQRS framework** in `platform-infrastructure/cqrs/` that provides:

1. **Command/Query Abstractions**
   - Base `Command` and `Query` interfaces
   - Type-safe command and query handlers
   - Command bus and query bus abstractions

2. **Handler Registration**
   - Automatic handler discovery via CDI (Quarkus Arc)
   - Type-safe handler resolution at runtime
   - Support for multiple handlers per command/query type

3. **Cross-Cutting Concerns**
   - Validation interceptors for commands
   - Logging and tracing integration
   - Transaction management per command
   - Audit trail for all commands

4. **Implementation Strategy**
   - Each bounded context implements its own handlers
   - Handlers reside in the `*-application/` layer
   - Domain logic stays in `*-domain/` layer
   - Infrastructure adapters in `*-infrastructure/` layer

## Consequences

### Positive
- ✅ Clear separation between reads and writes
- ✅ Simplified testing of business logic
- ✅ Scalable read and write models independently
- ✅ Explicit business operations via commands
- ✅ Consistent error handling and validation
- ✅ Easy to add cross-cutting concerns (auth, logging, metrics)

### Negative
- ❌ Additional abstraction layer increases initial complexity
- ❌ Learning curve for developers new to CQRS
- ❌ Potential over-engineering for simple CRUD operations
- ❌ Need to maintain handler registration and routing

### Neutral
- ⚖️ Opens path to event sourcing if needed
- ⚖️ Can migrate to separate read/write databases later
- ⚖️ Provides foundation for distributed sagas

## Implementation Notes

### Command Example
```kotlin
// In financial-accounting/accounting-application
data class CreateAccountCommand(
    val accountNumber: String,
    val accountName: String,
    val accountType: AccountType,
    val tenantId: TenantId
) : Command<AccountId>

@ApplicationScoped
class CreateAccountHandler(
    private val accountRepository: AccountRepository,
    private val eventPublisher: EventPublisher
) : CommandHandler<CreateAccountCommand, AccountId> {
    
    @Transactional
    override fun handle(command: CreateAccountCommand): AccountId {
        // Domain logic
        val account = Account.create(
            command.accountNumber,
            command.accountName,
            command.accountType
        )
        
        accountRepository.save(account)
        eventPublisher.publish(AccountCreatedEvent(account.id))
        
        return account.id
    }
}
```

### Query Example
```kotlin
data class GetAccountQuery(
    val accountId: AccountId
) : Query<AccountDTO>

@ApplicationScoped
class GetAccountHandler(
    private val accountRepository: AccountRepository
) : QueryHandler<GetAccountQuery, AccountDTO> {
    
    override fun handle(query: GetAccountQuery): AccountDTO {
        val account = accountRepository.findById(query.accountId)
            ?: throw AccountNotFoundException(query.accountId)
        
        return AccountDTO.from(account)
    }
}
```

## Alternatives Considered

### 1. Direct Service Methods
**Rejected**: Lack of consistency, no centralized cross-cutting concerns, harder to test and audit.

### 2. Full Event Sourcing from Day One
**Rejected**: Too complex for initial implementation. CQRS provides foundation to add event sourcing later if needed.

### 3. Third-Party CQRS Framework (Axon, etc.)
**Rejected**: Vendor lock-in, heavy dependencies, reduced control over implementation. Our needs are specific enough to warrant custom implementation.

## References

- [Martin Fowler: CQRS](https://martinfowler.com/bliki/CQRS.html)
- [Greg Young: CQRS Documents](https://cqrs.files.wordpress.com/2010/11/cqrs_documents.pdf)
- `platform-infrastructure/cqrs/` implementation
- `docs/ARCHITECTURE.md` - CQRS section
