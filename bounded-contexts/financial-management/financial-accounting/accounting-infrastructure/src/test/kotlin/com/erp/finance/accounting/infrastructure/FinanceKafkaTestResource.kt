package com.erp.finance.accounting.infrastructure

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class FinanceKafkaTestResource : QuarkusTestResourceLifecycleManager {
    private lateinit var kafka: KafkaContainer

    override fun start(): Map<String, String> {
        val withContainers = System.getProperty("withContainers", "false")
        if (withContainers != "true") {
            return emptyMap()
        }

        kafka =
            KafkaContainer(
                DockerImageName.parse("confluentinc/cp-kafka:7.6.1"),
            ).apply {
                withReuse(true)
                start()
            }

        val bootstrap = kafka.bootstrapServers
        System.setProperty("KAFKA_BOOTSTRAP_SERVERS", bootstrap)
        return mapOf(
            "KAFKA_BOOTSTRAP_SERVERS" to bootstrap,
            "mp.messaging.outgoing.finance-journal-events-out.bootstrap.servers" to bootstrap,
            "mp.messaging.outgoing.finance-period-events-out.bootstrap.servers" to bootstrap,
            "mp.messaging.outgoing.finance-dimension-events-out.bootstrap.servers" to bootstrap,
        )
    }

    override fun stop() {
        if (::kafka.isInitialized) {
            kafka.stop()
            System.clearProperty("KAFKA_BOOTSTRAP_SERVERS")
        }
    }
}
