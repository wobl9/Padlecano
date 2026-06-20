package com.example.padlecano.domain.model

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

typealias EntityId = String

@OptIn(ExperimentalUuidApi::class)
fun newEntityId(): EntityId {
    return Uuid.random().toString()
}
