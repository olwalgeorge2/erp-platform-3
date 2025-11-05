# ADR-003: Event-Driven Integration Between Contexts

**Status**: Accepted  
**Date**: 2025-11-05  
**Deciders**: Architecture Team  
**Tags**: events, integration, messaging, bounded-contexts

## Context

Bounded contexts in our ERP platform need to share data and coordinate workflows without creating tight coupling. We must choose an integration strategy that supports eventual consistency, scalability, and maintains bounded context autonomy.

## Decision

We will use **asynchronous event-driven integration** as the primary communication pattern between bounded contexts, with **synchronous REST APIs** reserved for read-heavy, request-response scenarios.

### Event-Driven Integration Strategy

1. **Domain Events as Integration Mechanism**
   - Each context publishes domain events for state changes
   - Events represent facts that have occurred
   - Published to `platform-shared/common-messaging` abstraction
   - Other contexts subscribe to relevant events

2. **Event Schema Ownership**
   - Publishing context owns event schema
   - Events published in `*-shared/` modules
   - Versioned contracts with backward compatibility
   - No breaking changes without migration path

3. **Message Broker**
   - **Apache Kafka (KRaft mode)** for production (ZooKeeper-less quorum)
   - Durable, ordered, replayable event log
   - Topic per event type or per bounded context
   - Consumer groups for parallel processing

4. **Event Store (Optional per Context)**
   - Contexts can optionally persist events
   - Enables event sourcing, audit trails, temporal queries
   - Implemented in `platform-infrastructure/eventing`

## Rationale

### Why Event-Driven?
- ✅ **Loose Coupling**: Contexts don't know about subscribers
- ✅ **Scalability**: Asynchronous processing, parallel consumers
- ✅ **Resilience**: Retry logic, dead letter queues, fault tolerance
- ✅ **Audit Trail**: Complete history of system changes
- ✅ **Temporal Decoupling**: Producer/consumer can be offline

### When to Use Synchronous APIs?
- 🔄 **Real-time queries** (e.g., "Get current stock level")
- 🔄 **User-facing operations** requiring immediate response
- 🔄 **Reference data lookups** (cached on consumer side)

### When to Use Events?
- 📢 **State change notifications** (OrderPlaced, AccountCreated)
- 📢 **Workflow orchestration** (multi-step sagas)
- 📢 **Data synchronization** across contexts
- 📢 **Analytics and reporting** (BI context consumes all events)

## Consequences

### Positive
- ✅ Bounded contexts remain independent
- ✅ Easy to add new event subscribers
- ✅ Natural audit log and debugging trail
- ✅ Supports event sourcing and CQRS
- ✅ Can replay events for new read models
- ✅ Resilient to temporary service outages

### Negative
- ❌ Eventual consistency (not immediate)
- ❌ Debugging distributed workflows is harder
- ❌ Need monitoring for event processing delays
- ❌ Dead letter queue management required
- ❌ Event schema evolution complexity
- ❌ Duplicate message handling required (idempotency)

### Neutral
- ⚖️ Requires message broker infrastructure (Kafka KRaft cluster)
- ⚖️ Developers must understand async patterns
- ⚖️ Testing integration flows more complex

## Implementation Details

### Event Definition
```kotlin
// In commerce-shared/order-shared/
package com.erp.commerce.order.events

import java.time.Instant
import java.util.UUID

data class OrderPlacedEvent(
    val eventId: UUID = UUID.randomUUID(),
    val occurredAt: Instant = Instant.now(),
    val orderId: OrderId,
    val customerId: CustomerId,
    val orderLines: List<OrderLineDTO>,
    val totalAmount: Money,
    val version: Int = 1
) : DomainEvent
```

### Publishing Events
```kotlin
// In commerce-ecommerce/application layer
@ApplicationScoped
class PlaceOrderHandler(
    private val orderRepository: OrderRepository,
    private val eventPublisher: EventPublisher
) : CommandHandler<PlaceOrderCommand, OrderId> {
    
    @Transactional
    override fun handle(command: PlaceOrderCommand): OrderId {
        val order = Order.create(command.customerId, command.lines)
        orderRepository.save(order)
        
        // Publish event
        eventPublisher.publish(
            OrderPlacedEvent(
                orderId = order.id,
                customerId = order.customerId,
                orderLines = order.lines.map { OrderLineDTO.from(it) },
                totalAmount = order.totalAmount
            )
        )
        
        return order.id
    }
}
```

### Consuming Events
```kotlin
// In inventory-stock/application layer
@ApplicationScoped
class OrderEventConsumer(
    private val inventoryService: InventoryService
) {
    
    @ConsumeEvent("commerce.OrderPlaced")
    @Transactional
    fun onOrderPlaced(event: OrderPlacedEvent) {
        logger.info("Reserving inventory for order ${event.orderId}")
        
        try {
            inventoryService.reserveStock(
                orderId = event.orderId,
                items = event.orderLines.map { 
                    StockReservation(it.productId, it.quantity) 
                }
            )
        } catch (e: InsufficientStockException) {
            // Publish compensating event
            eventPublisher.publish(
                StockReservationFailedEvent(event.orderId, e.message)
            )
        }
    }
}
```

### Event Versioning
```kotlin
// Version 1
data class OrderPlacedEvent(
    val orderId: OrderId,
    val customerId: CustomerId,
    val totalAmount: Money,
    val version: Int = 1
)

// Version 2 - added field
data class OrderPlacedEvent(
    val orderId: OrderId,
    val customerId: CustomerId,
    val totalAmount: Money,
    val shippingAddress: Address?, // New field, nullable for backward compatibility
    val version: Int = 2
)

// Consumer handles both versions
when (event.version) {
    1 -> handleV1(event)
    2 -> handleV2(event)
}
```

## Message Broker Configuration

### Kafka Topics Strategy

**Option 1: Topic per Event Type**
```
commerce.order.placed
commerce.order.cancelled
inventory.stock.reserved
financial.payment.completed
```

**Option 2: Topic per Bounded Context** (Chosen)
```
commerce-events (all commerce events)
inventory-events
financial-events
```

**Rationale**: Option 2 provides better ordering guarantees within a context and simpler topic management.

### Consumer Groups
- One consumer group per subscribing bounded context
- Enables parallel processing within a context
- Independent consumption rates

### Kafka Configuration
```yaml
# In application.properties
kafka.bootstrap.servers=localhost:9092

# Producer config
mp.messaging.outgoing.commerce-events.connector=smallrye-kafka
mp.messaging.outgoing.commerce-events.topic=commerce-events
mp.messaging.outgoing.commerce-events.value.serializer=io.quarkus.kafka.client.serialization.JsonbSerializer

# Consumer config
mp.messaging.incoming.commerce-events.connector=smallrye-kafka
mp.messaging.incoming.commerce-events.topic=commerce-events
mp.messaging.incoming.commerce-events.value.deserializer=io.quarkus.kafka.client.serialization.JsonbDeserializer
mp.messaging.incoming.commerce-events.group.id=inventory-consumer
```

KRaft mode eliminates ZooKeeper. Our broker configuration uses combined broker/controller roles:

```properties
process.roles=broker,controller
controller.listener.names=CONTROLLER
listener.security.protocol.map=PLAINTEXT:PLAINTEXT,CONTROLLER:PLAINTEXT
controller.quorum.voters=1@localhost:9093
```

## Idempotency

All event handlers MUST be idempotent:

```kotlin
@ConsumeEvent("commerce.OrderPlaced")
@Transactional
fun onOrderPlaced(event: OrderPlacedEvent) {
    // Check if already processed
    if (processedEvents.exists(event.eventId)) {
        logger.debug("Event ${event.eventId} already processed, skipping")
        return
    }
    
    // Process event
    inventoryService.reserveStock(event.orderId, event.orderLines)
    
    // Mark as processed
    processedEvents.record(event.eventId)
}
```

## Saga Pattern for Distributed Transactions

For multi-step workflows:

```kotlin
// Choreography-based saga
OrderPlaced -> ReserveInventory -> ProcessPayment -> ShipOrder

// Each step publishes success/failure events
// Compensating transactions on failure
PaymentFailed -> ReleaseInventory -> CancelOrder
```

## Monitoring & Observability

- **Event Publishing Metrics**: Events published per context, failures
- **Consumer Lag**: How far behind consumers are
- **Dead Letter Queue**: Failed events requiring manual intervention
- **Processing Time**: P50, P95, P99 per event type
- **Event Replay**: Track replayed events separately

## Alternatives Considered

### 1. Synchronous REST Only
**Rejected**: Creates tight coupling, cascading failures, difficult to scale independently.

### 2. Shared Database Integration
**Rejected**: Violates bounded context autonomy, creates coupling at data level.

### 3. RabbitMQ Instead of Kafka
**Deferred**: RabbitMQ excellent for queuing, but Kafka's log-based approach better for event sourcing and replay. May use RabbitMQ for specific use cases.

### 4. AWS EventBridge / Azure Event Grid
**Deferred**: Cloud-specific, vendor lock-in. Will consider for cloud deployments but keep abstraction layer.

## Related ADRs

- ADR-001: Modular CQRS Implementation
- ADR-002: Database Per Bounded Context
- ADR-004: API Gateway Pattern (to be written)
- ADR-006: Saga Pattern for Distributed Transactions (to be written)

## Testing Strategy

- **Unit Tests**: Mock `EventPublisher` interface
- **Integration Tests**: Use Testcontainers for Kafka (KRaft-enabled `KafkaContainer`)
- **Contract Tests**: Verify event schema compatibility
- **E2E Tests**: Full workflow testing with real broker

## Review Date

- **After Phase 4**: Review event schema versioning approach
- **After Phase 6**: Assess if saga orchestration needed
- **Quarterly**: Review Kafka vs alternatives based on operational experience
