package com.example.padlecano.domain.model

import java.util.UUID

typealias EntityId = String

fun newEntityId(): EntityId {
    return UUID.randomUUID().toString()
}
