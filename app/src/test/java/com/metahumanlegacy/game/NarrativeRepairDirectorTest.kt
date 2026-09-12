package com.metahumanlegacy.game

import org.junit.Assert.*
import org.junit.Test

class NarrativeRepairDirectorTest {
    @Test fun completed_arc_is_replaced_by_a_life_interlude() {
        val c = Campaign(
            seed = 91L, name = "A", modifier = "", turn = 220,
            flags = setOf("POWER_REVEALED", "ARC_TEST_COMPLETE")
        )
        val resurrected = EventNode(
            id = "old_arc_stage_1", title = "Ancien arc", text = "Déjà terminé",
            choices = listOf(Choice("Rejouer")), category = "DESTIN", provocation = "ARC",
            stakes = 1, threadId = "ARC_TEST", threadStage = 1, kind = "MAJOR"
        )
        val repaired = NarrativeRepairDirector.repair(c, resurrected)
        assertTrue(repaired.id.startsWith("LIFE_INTERLUDE_"))
        assertNull(repaired.threadId)
        assertEquals("QUIET", repaired.kind)
        assertEquals(4, repaired.choices.size)
    }

    @Test fun active_arc_is_preserved() {
        val c = Campaign(seed = 92L, name = "A", modifier = "", turn = 40, flags = setOf("POWER_REVEALED"))
        val active = EventNode(
            id = "arc_stage_2", title = "Arc actif", text = "Continue",
            choices = listOf(Choice("Continuer")), category = "DESTIN", provocation = "ARC",
            stakes = 2, threadId = "ARC_TEST", threadStage = 2, kind = "MAJOR"
        )
        assertEquals(active, NarrativeRepairDirector.repair(c, active))
    }
}
