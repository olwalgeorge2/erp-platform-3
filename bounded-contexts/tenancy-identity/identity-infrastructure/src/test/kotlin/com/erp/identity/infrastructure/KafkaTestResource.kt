package com.erp.identity.infrastructure

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.utility.DockerImageName

class KafkaTestResource : QuarkusTestResourceLifecycleManager {
    private lateinit var kafka: KafkaContainer

    override fun start(): Map<String, String> {
        kafka =
            KafkaContainer(DockerImageName.parse("docker.redpanda.com/redpandadata/redpanda:v24.2.11")).apply {
                withReuse(true)
                start()
            }

        val bootstrap = kafka.bootstrapServers
        return mapOf(
            // For property expansion in application.yaml
            "KAFKA_BOOTSTRAP_SERVERS" to bootstrap,
            // Direct channel overrides (explicit)
            "mp.messaging.incoming.identity-events-in.bootstrap.servers" to bootstrap,
            "mp.messaging.outgoing.identity-events-out.bootstrap.servers" to bootstrap,
        )
    }

    override fun stop() {
        if (::kafka.isInitialized) {
            kafka.stop()
        }
    }
}
