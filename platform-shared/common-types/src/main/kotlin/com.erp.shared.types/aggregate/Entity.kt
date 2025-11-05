package com.erp.shared.types.aggregate

/**
 * Base class for entities with an identity inside an aggregate boundary.
 */
abstract class Entity<ID>(
    open val id: ID,
)
