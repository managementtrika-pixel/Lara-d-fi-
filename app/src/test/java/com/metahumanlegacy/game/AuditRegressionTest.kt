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

    @Test fun runtime_does_not_mutate_the_same_relationship_twice() {
        val deep = DeepLifeState(seed = 1L, relationships = listOf(relationship("family", "Maya", "Sœur")))
        val before = Campaign(seed = 1L, name = "A", modifier = "")
        val event = EventNode("family_call", "Maya appelle", "Maya te demande de rentrer.", emptyList(), "RELATION", "", 2)
        val update = DeepLifeRuntime.afterChoice(before, before.copy(turn = 1), event, Choice("Rentrer", relationDelta = 3, approach = "CARE"), deep)
        assertEquals(56, update.state.relationships.single().trust)
    }

    @Test fun ignored_relationship_opportunity_has_a_real_cost() {
        val family = relationship("family", "Maya", "Sœur")
        val deep = DeepLifeState(
            seed = 11L,
            relationships = listOf(family),
            opportunities = listOf(Opportunity("missed", "Voir Maya", "RELATION", expiresTurn = 0, urgency = 3, personId = "family", ignoredPayload = "Maya n'attend plus."))
        )
        val before = Campaign(seed = 11L, name = "A", modifier = "", turn = 0)
        val after = before.copy(turn = 1)
        val event = EventNode("weather", "Une journée ordinaire", "Rien ne concerne Maya.", emptyList(), "QUIET", "", 1)
        val update = DeepLifeRuntime.afterChoice(before, after, event, Choice("Continuer"), deep)
        val next = update.state.relationships.single()
        assertEquals(45, next.trust)
        assertEquals(47, next.affection)
        assertTrue(next.grudge >= 4)
        assertTrue(update.echo.contains("Maya n'attend plus"))
    }

    @Test fun direct_relationship_action_consumes_time_and_changes_the_person() {
        val c = Campaign(seed = 12L, name = "A", modifier = "", turn = 20)
        val annual = AnnualActionState.fresh(c)
        val deep = DeepLifeState(seed = 12L, relationships = listOf(relationship("friend", "Noa", "Ami")))
        val card = AnnualActionCard(
            id = "direct_rel_visit_friend", title = "Passer du temps avec Noa", description = "", category = AnnualActionCategory.RELATION,
            iconKey = "relation_family", focus = "Lien", outcome = "Présence"
        )
        val action = AnnualActionEngine.perform(c, annual, card)
        assertNotNull(action)
        assertEquals(1, action!!.state.used)
        val update = DeepLifeRuntime.afterAnnualAction(c, card, deep)
        val noa = update.state.relationships.single()
        assertEquals(55, noa.trust)
        assertEquals(56, noa.affection)
        assertTrue(noa.memories.any { it.eventId == "DIRECT_RELATION" })
    }

    @Test fun world_layer_mirrors_deep_relationship_instead_of_diverging() {
        val c = GameEngine.newCampaign(91L).copy(
            turn = 20, powerFamily = "Énergie", flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
        )
        val baseUltimate = UltimateStore.fallback(c)
        val source = baseUltimate.relation("family")!!
        val deep = DeepLifeState(seed = c.seed, relationships = listOf(
            DeepRelationship("family", source.name, source.role, trust = 81, affection = 77, grudge = 6)
        ))
        val event = EventNode("quiet", "Soir calme", "Rien ne vise ta famille.", listOf(Choice("Continuer")), "QUIET", "", 1)
        val world = DeepWorldDirector.afterChoice(c, baseUltimate, deep, event, event.choices.first())
        val mirrored = world.ultimate.relation("family")!!
        assertEquals(81, mirrored.trust)
        assertEquals(77, mirrored.affection)
        assertEquals(6, mirrored.grudge)
    }

    @Test fun nemesis_only_learns_faster_when_an_approach_is_actually_repeated() {
        val c = GameEngine.newCampaign(92L).copy(
            turn = 30, powerFamily = "Énergie", flags = setOf("POWER_REVEALED", "ALIAS_CHOSEN")
        )
        val ultimate = UltimateStore.fallback(c).copy(nemesis = "Vanta", nemesisAdaptation = 20)
        val priorCare = CharacterMemory("prior", 27, c.age, eventId = "old", summary = "old", tags = setOf("CARE"))
        val deep = DeepLifeState(seed = c.seed, memories = listOf(priorCare))
        val event = EventNode("rival", "Retour de Vanta", "Vanta observe.", listOf(Choice("Changer", approach = "ORDER"), Choice("Répéter", approach = "CARE")), "RIVAL", "", 3)
        val changed = DeepWorldDirector.afterChoice(c, ultimate, deep, event, event.choices[0])
        val repeated = DeepWorldDirector.afterChoice(c, ultimate, deep, event, event.choices[1])
        assertEquals(22, changed.ultimate.nemesisAdaptation)
        assertEquals(24, repeated.ultimate.nemesisAdaptation)
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

    @Test fun weaknesses_change_power_choices_not_just_flavour_text() {
        val powerChoice = Choice("Utiliser le pouvoir", power = 2, risk = 3, identityDelta = 0, flag = "v2_power_test")
        val base = Campaign(seed = 9L, name = "A", modifier = "", powerFamily = "Énergie", flags = setOf("POWER_REVEALED"))

        val fatigue = PowerGameplayDirector.applyWeakness(base.copy(weakness = "Fatigue extrême"), powerChoice)
        assertTrue(fatigue.healthDelta < powerChoice.healthDelta)
        assertTrue(fatigue.risk > powerChoice.risk)

        val visibility = PowerGameplayDirector.applyWeakness(base.copy(weakness = "Pouvoir difficile à dissimuler"), powerChoice)
        assertTrue(visibility.identityDelta > powerChoice.identityDelta)

        val cooldown = PowerGameplayDirector.applyWeakness(base.copy(weakness = "Temps de récupération"), powerChoice)
        assertTrue(cooldown.deferredHook)

        val overload = PowerGameplayDirector.applyWeakness(base.copy(weakness = "Surcharge"), powerChoice)
        assertTrue(overload.power > powerChoice.power)
        assertTrue(overload.risk > powerChoice.risk)
    }

    @Test fun weakness_does_not_penalise_a_non_power_choice() {
        val quiet = Choice("Parler", power = 0, risk = 1, flag = "social")
        val c = Campaign(seed = 10L, name = "A", modifier = "", weakness = "Surcharge", flags = setOf("POWER_REVEALED"))
        assertEquals(quiet, PowerGameplayDirector.applyWeakness(c, quiet))
    }

    @Test fun player_facing_outcome_no_longer_dumps_hidden_stat_deltas() {
        val before = Campaign(seed = 13L, name = "A", modifier = "", powerFamily = "Énergie", flags = setOf("POWER_REVEALED"))
        val after = before.copy(health = 90, opinion = 5, identityExposure = 4, influence = 4)
        val event = EventNode("scene", "Intervention", "Une crise.", emptyList(), "CRISE", "", 4, threadStage = 2)
        val text = GameRules.outcome(before, after, event, Choice("Agir", moral = 2, prestige = 2, opinion = 3, risk = 6, approach = "CARE"))
        assertFalse(text.contains("moralité +"))
        assertFalse(text.contains("prestige +"))
        assertFalse(text.contains("santé -"))
        assertTrue(text.contains("corps") || text.contains("traces") || text.contains("personnes"))
    }
}
