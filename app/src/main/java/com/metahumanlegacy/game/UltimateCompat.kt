package com.metahumanlegacy.game

import java.util.Collections
import java.util.Random

/** Keeps deterministic seeded Java Random usage consistent across the legacy engine and ultimate layer. */
internal fun <T> List<T>.shuffled(random: Random): List<T> {
    val copy = toMutableList()
    Collections.shuffle(copy, random)
    return copy
}
