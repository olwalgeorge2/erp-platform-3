package com.erp.apigateway.infrastructure

import io.quarkus.redis.datasource.RedisDataSource
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class RedisService {
    @Inject
    lateinit var redis: RedisDataSource

    private val stringCommands by lazy { redis.value(String::class.java) }
    private val longCommands by lazy { redis.value(Long::class.java) }
    private val keyCommands by lazy { redis.key() }
    private val stringCommands by lazy { redis.value(String::class.java) }

    fun incr(key: String): Long = longCommands.incr(key)

    fun expire(
        key: String,
        seconds: Long,
    ) {
        redis.key().expire(key, seconds)
    }

    fun get(key: String): String? = stringCommands.get(key)

    fun set(
        key: String,
        value: String,
    ) {
        stringCommands.set(key, value)
    }

    fun del(key: String) {
        keyCommands.del(key)
    }

    fun keys(pattern: String): List<String> = keyCommands.keys(pattern)
}
