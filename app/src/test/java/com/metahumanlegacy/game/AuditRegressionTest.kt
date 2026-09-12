package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class AuditRegressionTest {
    private fun relationship(id: String, name: String, role: String) = DeepRelationship(id = id, name = name, role = role)

    @Test fun unrelated_scene_does_not_invent_a_present_person() {
        val deep = DeepLifeState(seed = 1L, relationships = listOf(
            relationship("family", "Maya", "Sœur"), relationship("journalist", "Noa", "Journaliste")
        ))
        val event = EventNode("storm", "Orage sur les quais", "La foudre coupe le réseau.", emptyList(), "CRISE", "", 3)
        assertNull(SceneContextDirector.participantId(event, deep))
    }

    @Test fun explicit_person_is_bound_to_scene_and_receives_memory() {
        val deep = DeepLifeState(seed = 1L, relationships = listOf(
            relationship("family", "Maya", "Sœur"), relationship("journalist", "Noa", "Journaliste")
        ))
        val event = EventNode("family_call", "Maya appelle", "Maya te demande de rentrer.", emptyList(), "RELATION", "", 2)
        assertEquals("family", SceneContextDirector.participantId(event, deep))
        val before = Campaign(seed = 1L, name = "A", modifier = "")
        val choice = Choice("Rentrer", relationDelta = 3, approach = "CARE")
        val after = DeepLifeDirector.afterChoice(before, before.copy(turn = 1), event, choice, deep)
        val maya = after.relationships.first { it.id == "family" }
        assertTrue(maya.memories.any { it.eventId == "family_call" && it.personId == "family" })
        assertTrue(maya.trust > 50)
        assertTrue(maya.affection > 50)
        assertTrue(after.memories.any { it.personId == "family" })
    }

    @Test fun retirement_path_is_reachable_before_natural_end() {
        val c68 = Campaign(seed = 2L, name = "A", modifier = "", turn = 208, flags = setOf("POWER_REVEALED", "V2_RETIREMENT_PATH"))
        assertEquals(68, c68.age)
        assertFalse(c68.finished)
        val base = EventNode("late", "Dernière ronde", "La ville appelle.", listOf(Choice("Intervenir")), "CRISE", "", 3)
        val enriched = LifeStageDirector.enrich(c68, base)
        assertTrue(enriched.choices.any { it.flag == "V2_RETIRE_NOW" })
        assertTrue(c68.copy(flags = c68.flags + "V2_RETIRED").finished)
    }

    @Test fun natural_life_ceiling_is_age_eighty() {
        val c = Campaign(seed = 3L, name = "A", modifier = "", turn = 256)
        assertEquals(80, c.age)
        assertTrue(c.finished)
    }

    @Test fun tricky_power_names_do_not_fall_back_to_projector() {
        assertEquals(PowerArchitecture.MENTAL, DeepLifeDirector.architectureFor("Télékinésie"))
        assertEquals(PowerArchitecture.MENTAL, DeepLifeDirector.architectureFor("Précognition limitée"))
        assertEquals(PowerArchitecture.TECH, DeepLifeDirector.architectureFor("Cybernétique"))
        assertEquals(PowerArchitecture.OCCULT, DeepLifeDirector.architectureFor("Invocation"))
        assertEquals(PowerArchitecture.MATTER, DeepLifeDirector.architectureFor("Absorption"))
        assertEquals(PowerArchitecture.MATTER, DeepLifeDirector.architectureFor("Duplication"))
        assertEquals(PowerArchitecture.MOBILITY, DeepLifeDirector.architectureFor("Portails limités"))
        assertEquals(PowerArchitecture.ADAPTIVE, DeepLifeDirector.architectureFor("Métamorphose défensive"))
    }

    @Test fun every_catalog_power_has_an_intentional_architecture() {
        val allowedProjectors = setOf("Électricité", "Feu", "Glace", "Énergie", "Lumière", "Plasma", "Chaleur contrôlée")
        PowerResolver.powerCatalog().forEach { power ->
            val architecture = DeepLifeDirector.architectureFor(power)
            if (architecture == PowerArchitecture.PROJECTOR) {
                assertTrue("Unexpected PROJECTOR fallback for $power", power in allowedProjectors)
            }
        }
    }
}
