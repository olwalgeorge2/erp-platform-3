# Redpanda Migration Guide

## Overview

The ERP platform has been migrated from **Apache Kafka with KRaft** to **Redpanda** for improved performance, simplicity, and operational efficiency.

## What Changed

### Infrastructure
- **Before**: Apache Kafka 3.8.1 with KRaft mode
- **After**: Redpanda v24.2.11

### Ports
- **Kafka API (external)**: `9092` → `19092`
- **Schema Registry**: Now built-in at `18081`
- **HTTP Proxy**: Now built-in at `18082`
- **Admin API**: Available at `19644`

### Management UI
- **Before**: Provectus Kafka UI
- **After**: Redpanda Console (official, more features)

## Why Redpanda?

### Performance Benefits
- ⚡ **10x faster** than Kafka in many scenarios
- 🚀 **Lower latency** - microseconds vs milliseconds
- 💾 **Less memory** - ~75% reduction (no JVM)
- ⏱️ **Faster startup** - seconds vs minutes

### Operational Benefits
- ✅ **Simpler setup** - single binary, no complex configuration
- ✅ **No ZooKeeper** - never needed (built with Raft from day one)
- ✅ **Auto-tuning** - optimizes for your hardware automatically
- ✅ **Built-in features**:
  - Schema Registry (compatible with Confluent)
  - HTTP Proxy for REST API access
  - WebAssembly data transforms

### Compatibility
- ✅ **100% Kafka API compatible** - drop-in replacement
- ✅ Works with all Kafka clients (Java, Python, Go, Node.js, etc.)
- ✅ Works with Kafka ecosystem tools
- ✅ No code changes required

## Configuration Changes

### Application Configuration

**Before (Kafka):**
```yaml
mp:
  messaging:
    outgoing:
      identity-events-out:
        bootstrap:
          servers: localhost:9092
```

**After (Redpanda):**
```yaml
mp:
  messaging:
    outgoing:
      identity-events-out:
        bootstrap:
          servers: localhost:19092  # Note: external port changed
```

### Docker Compose

The `docker-compose-kafka.yml` file now uses:
- `redpanda` service instead of `kafka`
- `redpanda-console` instead of `kafka-ui`
- Built-in Schema Registry (no separate service needed)

## Usage

### Starting Services

```powershell
# Start Redpanda, Console, and PostgreSQL
docker-compose -f docker-compose-kafka.yml up -d

# Check health
docker-compose -f docker-compose-kafka.yml ps

# View logs
docker-compose -f docker-compose-kafka.yml logs -f redpanda
```

### Accessing Services

| Service | URL | Description |
|---------|-----|-------------|
| Redpanda Console | http://localhost:8090 | Web UI for topics, consumers, messages |
| Kafka API | localhost:19092 | Kafka protocol endpoint |
| Schema Registry | http://localhost:18081 | Schema management |
| HTTP Proxy | http://localhost:18082 | REST API for Kafka |
| Admin API | http://localhost:19644 | Redpanda admin operations |

### Using Redpanda CLI (`rpk`)

```powershell
# Execute rpk commands inside the container
docker exec -it erp-redpanda rpk cluster info
docker exec -it erp-redpanda rpk topic list
docker exec -it erp-redpanda rpk topic create my-topic
docker exec -it erp-redpanda rpk topic describe identity.domain.events.v1

# Produce a message
docker exec -it erp-redpanda rpk topic produce identity.domain.events.v1

# Consume messages
docker exec -it erp-redpanda rpk topic consume identity.domain.events.v1
```

## Testing

### Testcontainers Configuration

Tests use Testcontainers with Redpanda for full Kafka API compatibility:

```kotlin
// KafkaTestResource.kt
class KafkaTestResource : QuarkusTestResourceLifecycleManager {
    private lateinit var kafka: KafkaContainer

    override fun start(): Map<String, String> {
        kafka = KafkaContainer(DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v24.2.11"))
            .apply {
                withReuse(true)
                start()
            }
        // Configuration...
    }
}
```

Note: Testcontainers uses Redpanda but exposes it via the standard Kafka API, so existing test code works without changes.

## Migration Checklist

- [x] Updated `docker-compose-kafka.yml` to use Redpanda
- [x] Changed Kafka API port from `9092` to `19092` (external)
- [x] Updated application configuration files
- [x] Migrated to Redpanda Console
- [x] Verified Testcontainers compatibility
- [x] Updated documentation

## Rollback Plan

If needed, you can rollback by:

1. Stop current services:
   ```powershell
   docker-compose -f docker-compose-kafka.yml down -v
   ```

2. Checkout previous version of `docker-compose-kafka.yml`

3. Revert application configuration port changes (`19092` → `9092`)

4. Restart services

## Performance Comparison

Expected improvements with Redpanda:

| Metric | Apache Kafka | Redpanda | Improvement |
|--------|--------------|----------|-------------|
| Memory Usage | ~4GB per broker | ~1GB per broker | 75% reduction |
| CPU Usage | ~2 cores | ~1 core | 50% reduction |
| P99 Latency | 50-100ms | 5-10ms | 10x faster |
| Startup Time | 30-60s | 3-5s | 10x faster |

## Additional Features

### Built-in Schema Registry

Redpanda includes a Schema Registry compatible with Confluent:

```powershell
# List schemas
curl http://localhost:18081/subjects

# Register a schema
curl -X POST http://localhost:18081/subjects/my-topic-value/versions \
  -H "Content-Type: application/vnd.schemaregistry.v1+json" \
  -d '{"schema": "{\"type\":\"record\",\"name\":\"User\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}"}'
```

### HTTP Proxy

Send/receive messages via REST API:

```powershell
# Produce via HTTP
curl -X POST http://localhost:18082/topics/identity.domain.events.v1 \
  -H "Content-Type: application/vnd.kafka.json.v2+json" \
  -d '{"records":[{"value":{"event":"test"}}]}'

# Create consumer
curl -X POST http://localhost:18082/consumers/my-group \
  -H "Content-Type: application/vnd.kafka.v2+json" \
  -d '{"name":"my-consumer","format":"json"}'
```

## Monitoring

### Redpanda Console Features
- 📊 Topic overview with message counts
- 🔍 Message browser with search/filter
- 👥 Consumer group monitoring
- 📈 Performance metrics
- 🎯 Schema registry management
- ⚙️ Cluster configuration

### Metrics Endpoints

Redpanda exposes Prometheus-compatible metrics:

```powershell
# Public metrics
curl http://localhost:19644/public_metrics

# Internal metrics (more detailed)
curl http://localhost:19644/metrics
```

## Troubleshooting

### Connection Issues

If services can't connect to Redpanda:

1. Check if Redpanda is running:
   ```powershell
   docker ps | findstr redpanda
   ```

2. Verify health:
   ```powershell
   docker exec -it erp-redpanda rpk cluster health
   ```

3. Check logs:
   ```powershell
   docker logs erp-redpanda
   ```

### Port Conflicts

If you see "address already in use" errors:

```powershell
# Check what's using the port
netstat -ano | findstr :19092

# Stop conflicting service or change Redpanda ports in docker-compose
```

### Data Persistence

Redpanda data is stored in the `redpanda-data` volume:

```powershell
# List volumes
docker volume ls | findstr redpanda

# Inspect volume
docker volume inspect erp-platform_redpanda-data

# Remove volume (WARNING: deletes all data)
docker-compose -f docker-compose-kafka.yml down -v
```

## Resources

- [Redpanda Documentation](https://docs.redpanda.com/)
- [Redpanda vs Kafka Comparison](https://redpanda.com/compare/redpanda-vs-kafka)
- [RPK CLI Reference](https://docs.redpanda.com/current/reference/rpk/)
- [Redpanda Console](https://docs.redpanda.com/current/console/)
- [Migration Guide](https://docs.redpanda.com/current/migrate/migrate-from-kafka/)

## Support

For issues or questions:
1. Check [Redpanda Documentation](https://docs.redpanda.com/)
2. Review [GitHub Issues](https://github.com/redpanda-data/redpanda/issues)
3. Join [Redpanda Slack Community](https://redpanda.com/slack)
