package com.metahumanlegacy.game

/**
 * Resolves who is actually involved in a scene. This deliberately avoids the old turn-based
 * round-robin that could put an unrelated NPC on screen simply because of the current turn.
 */
internal object SceneContextDirector {
    fun participantId(event: EventNode, state: DeepLifeState): String? {
        val alive = state.relationships.filter { it.alive }
        if (alive.isEmpty()) return null

        val searchable = listOf(event.id, event.title, event.text, event.category, event.threadId.orEmpty())
            .joinToString(" ").lowercase()

        // Strongest signal: the authored scene explicitly references a known person/name/role/id.
        alive.firstOrNull { person ->
            searchable.contains(person.id.lowercase()) ||
                searchable.contains(person.name.lowercase()) ||
                person.role.takeIf { it.length >= 4 }?.let { searchable.contains(it.lowercase()) } == true
        }?.let { return it.id }

        // Existing memories/thread continuity are stronger than a generic category guess.
        alive.mapNotNull { person ->
            val score = person.memories.sumOf { memory ->
                when {
                    memory.eventId == event.id -> 8 + memory.weight
                    event.threadId != null && memory.tags.any { it.equals(event.threadId, true) } -> 5 + memory.weight
                    memory.tags.any { searchable.contains(it.lowercase()) } -> 2 + memory.weight
                    else -> 0
                }
            }
            person.takeIf { score > 0 }?.let { it to score }
        }.maxByOrNull { it.second }?.first?.let { return it.id }

        // Conservative semantic fallback. If no relationship is genuinely relevant, return null:
        // an empty presence card is better than inventing that somebody was there.
        val preferredIds = when {
            searchable.contains("famill") || searchable.contains("parent") || searchable.contains("maison") -> listOf("family")
            searchable.contains("ami") || searchable.contains("école") || searchable.contains("enfance") -> listOf("friend", "family")
            searchable.contains("journal") || searchable.contains("média") || searchable.contains("presse") -> listOf("journalist")
            searchable.contains("rival") || searchable.contains("némés") || searchable.contains("combat") -> listOf("rival")
            searchable.contains("mentor") || searchable.contains("entraîn") || searchable.contains("transmission") -> listOf("mentor")
            else -> emptyList()
        }
        return preferredIds.firstNotNullOfOrNull { id -> alive.firstOrNull { it.id == id }?.id }
    }

    fun participant(event: EventNode, state: DeepLifeState): DeepRelationship? {
        val id = participantId(event, state) ?: return null
        return state.relationships.firstOrNull { it.id == id && it.alive }
    }
}
