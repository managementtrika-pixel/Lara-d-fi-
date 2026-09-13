package com.metahumanlegacy.game

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private const val REBUILD_SESSION = "mhl_ultimate_session_v1"

private fun loadRebuildOutcome(context: Context, seed: Long?): String? {
    if (seed == null) return null
    return context.getSharedPreferences(REBUILD_SESSION, Context.MODE_PRIVATE).getString("outcome_$seed", null)
}

private fun saveRebuildOutcome(context: Context, seed: Long, value: String?) {
    val edit = context.getSharedPreferences(REBUILD_SESSION, Context.MODE_PRIVATE).edit()
    if (value == null) edit.remove("outcome_$seed") else edit.putString("outcome_$seed", value)
    edit.apply()
}

@Composable
internal fun GameplayRebuildApp(context: Context) {
    var motion by remember { mutableStateOf(MetahumanMotionPreferences.load(context)) }
    val controller = remember(motion) {
        MetahumanMotionController(motion) { next ->
            val clean = next.copy(textScalePercent = next.textScalePercent.coerceIn(90, 120))
            motion = clean
            MetahumanMotionPreferences.save(context, clean)
        }
    }
    val haptic = rememberMetahumanHaptic()
    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity.density, baseDensity.fontScale, motion.textScalePercent) {
        Density(baseDensity.density, baseDensity.fontScale * motion.textScalePercent / 100f)
    }
    val colors = darkColorScheme(
        background = if (motion.highContrast) Color.Black else MetahumanColors.Coal,
        surface = if (motion.highContrast) Color(0xFF05070A) else MetahumanColors.Panel,
        primary = UltimateGold,
        secondary = UltimateBlue,
        error = UltimateRed,
        onBackground = UltimateIvory,
        onSurface = UltimateIvory,
        onPrimary = Color.Black
    )

    CompositionLocalProvider(LocalMetahumanMotion provides controller, LocalDensity provides scaledDensity) {
        MaterialTheme(colorScheme = colors) {
            var campaign by remember { mutableStateOf(loadCampaignV4(context)) }
            var ultimate by remember { mutableStateOf(campaign?.let { UltimateStore.load(context, it) }) }
            var annual by remember { mutableStateOf(campaign?.let { AnnualActionPersistence.load(context, it) }) }
            var deep by remember {
                mutableStateOf(campaign?.let { c -> ultimate?.let { u -> DeepLifePersistence.load(context, c, u) } })
            }
            var screen by remember { mutableStateOf("HOME") }
            var hall by remember { mutableStateOf(loadHallV4(context)) }
            var outcome by remember { mutableStateOf(loadRebuildOutcome(context, campaign?.seed)) }
            var draftSeed by remember { mutableStateOf(System.currentTimeMillis()) }
            var blueprint by remember { mutableStateOf(GameEngine.randomBlueprint(draftSeed)) }
            var draft by remember { mutableStateOf(UltimateCatalog.randomDraft(draftSeed, blueprint)) }
            var savePulse by remember { mutableIntStateOf(0) }

            fun persist(c: Campaign, u: UltimateState, a: AnnualActionState? = annual, d: DeepLifeState? = deep) {
                campaign = c
                ultimate = u
                saveCampaignV4(context, c)
                UltimateStore.save(context, u)
                if (a != null) {
                    annual = a
                    AnnualActionPersistence.save(context, a)
                }
                if (d != null) {
                    deep = d
                    DeepLifePersistence.save(context, d)
                }
                savePulse++
            }

            fun newDraft() {
                draftSeed = System.currentTimeMillis() xor 0x5A17L
                blueprint = GameEngine.randomBlueprint(draftSeed)
                draft = UltimateCatalog.randomDraft(draftSeed, blueprint)
            }

            fun go(next: String, feedback: MetahumanMotionLevel = MetahumanMotionLevel.MOTION_SUBTLE) {
                if (screen != next) haptic(feedback)
                screen = next
            }

            fun startLife(d: UltimateCreationDraft) {
                val seed = System.currentTimeMillis()
                val c = GameEngine.newCampaign(seed, d.blueprint)
                val u = UltimateStore.create(c, d)
                val a = AnnualActionState.fresh(c)
                val baseDeep = DeepLifeDirector.bootstrap(c, u)
                val generationDeep = GenerationalDirector.seedNewLife(c, baseDeep, DeepLegacyArchive.load(context))
                val dl = generationDeep.copy(lifeSimulation = LifeSimulationDirector.bootstrap(c, generationDeep))
                campaign = c; ultimate = u; annual = a; deep = dl
                saveCampaignV4(context, c)
                UltimateStore.save(context, u)
                AnnualActionPersistence.save(context, a)
                DeepLifePersistence.save(context, dl)
                outcome = null
                saveRebuildOutcome(context, seed, null)
                savePulse++
                haptic(MetahumanMotionLevel.MOTION_STANDARD)
                screen = "DESTIN"
            }

            fun abandon() {
                campaign?.let {
                    UltimateStore.clear(context, it.seed)
                    AnnualActionPersistence.clear(context, it.seed)
                    DeepLifePersistence.clear(context, it.seed)
                    saveRebuildOutcome(context, it.seed, null)
                }
                clearCampaignV4(context)
                campaign = null; ultimate = null; annual = null; deep = null; outcome = null
                newDraft()
                screen = "CREATE"
            }

            UltimateRootBackdrop(campaign = campaign, state = ultimate, scene = if (campaign?.finished == true) "LEGACY" else screen) {
                val transitionDuration = MetahumanMotionTokens.duration(MetahumanMotionTokens.FAST, motion)
                val stageKey = "${screen}|${campaign?.turn ?: -1}|${outcome?.hashCode() ?: 0}|${campaign?.needsAlias == true}|$savePulse"
                AnimatedContent(
                    targetState = stageKey,
                    transitionSpec = {
                        if (motion.reduceMotion) {
                            fadeIn(tween(transitionDuration)) togetherWith fadeOut(tween(transitionDuration))
                        } else {
                            (fadeIn(tween(transitionDuration)) + slideInHorizontally(tween(transitionDuration)) { it / 12 }) togetherWith
                                (fadeOut(tween(transitionDuration)) + slideOutHorizontally(tween(transitionDuration)) { -it / 14 })
                        }
                    },
                    label = "gameplay-rebuild-stage"
                ) { targetStage ->
                    val renderedScreen = targetStage.substringBefore('|')
                    when {
                        renderedScreen == "SETTINGS" -> UltimateSettingsScreen(
                            settings = motion,
                            onChange = controller.update,
                            onBack = { go(if (campaign == null) "HOME" else "DESTIN") }
                        )

                        renderedScreen == "HALL" -> DeepLegacyHallScreen(
                            oldHallCount = hall.size,
                            records = DeepLegacyArchive.load(context),
                            onBack = { go("HOME") }
                        )

                        renderedScreen == "HOME" -> UltimateHomeScreen(
                            campaign = campaign,
                            state = ultimate,
                            hallCount = maxOf(hall.size, DeepLegacyArchive.load(context).size),
                            onContinue = { go(if (campaign?.needsAlias == true) "ALIAS" else "DESTIN", MetahumanMotionLevel.MOTION_STANDARD) },
                            onNew = { abandon() },
                            onHall = { go("HALL") },
                            onSettings = { go("SETTINGS") }
                        )

                        campaign == null && renderedScreen == "CREATE" -> UltimateCreateScreen(
                            draft = draft,
                            onDraft = { draft = it; blueprint = it.blueprint },
                            onRandomize = { haptic(MetahumanMotionLevel.MOTION_SUBTLE); newDraft() },
                            onBack = { go("HOME") },
                            onStart = { startLife(it) }
                        )

                        campaign == null -> UltimateHomeScreen(
                            campaign = null,
                            state = null,
                            hallCount = maxOf(hall.size, DeepLegacyArchive.load(context).size),
                            onContinue = { },
                            onNew = { go("CREATE", MetahumanMotionLevel.MOTION_STANDARD) },
                            onHall = { go("HALL") },
                            onSettings = { go("SETTINGS") }
                        )

                        campaign!!.finished -> UltimateFinalScreen(campaign!!, ultimate ?: UltimateStore.fallback(campaign!!)) {
                            val c = campaign!!
                            val u = ultimate ?: UltimateStore.fallback(c)
                            val dl = deep ?: DeepLifePersistence.load(context, c, u)
                            DeepLegacyArchive.archive(context, c, u, dl)
                            val entry = LegacyRecord.from(c, u).encode()
                            hall = (listOf(entry) + hall).distinctBy {
                                LegacyRecord.decode(it).identityId.ifBlank { LegacyRecord.decode(it).name + "|" + LegacyRecord.decode(it).title }
                            }.take(60)
                            saveHallV4(context, hall)
                            UltimateStore.clear(context, c.seed)
                            AnnualActionPersistence.clear(context, c.seed)
                            DeepLifePersistence.clear(context, c.seed)
                            saveRebuildOutcome(context, c.seed, null)
                            clearCampaignV4(context)
                            campaign = null; ultimate = null; annual = null; deep = null; outcome = null
                            newDraft()
                            haptic(MetahumanMotionLevel.MOTION_LEGENDARY)
                            screen = "HOME"
                        }

                        renderedScreen == "ALIAS" -> UltimateAliasScreen(campaign!!, ultimate ?: UltimateStore.fallback(campaign!!)) { alias, presentation, palette, mask ->
                            val c = GameEngine.setAlias(campaign!!, alias)
                            val u = (ultimate ?: UltimateStore.fallback(c)).copy(
                                heroPresentation = presentation,
                                costumePalette = palette,
                                maskStyle = mask,
                                costumeEra = maxOf(1, (ultimate ?: UltimateStore.fallback(c)).costumeEra)
                            )
                            persist(c, u)
                            haptic(MetahumanMotionLevel.MOTION_MAJOR)
                            screen = "DESTIN"
                        }

                        renderedScreen == "CHRONIQUE" -> {
                            val c = campaign!!
                            val u = ultimate ?: UltimateStore.fallback(c).also { ultimate = it }
                            val dl = (deep ?: DeepLifePersistence.load(context, c, u)).also { deep = it }
                            DeepLifeChronicleScreen(c, u, dl, onBack = { go("DESTIN") })
                        }

                        else -> {
                            val c = campaign!!
                            val u = ultimate ?: UltimateStore.fallback(c).also { ultimate = it }
                            val a = (annual ?: AnnualActionPersistence.load(context, c)).synced(c).also { annual = it }
                            val loadedDeep = (deep ?: DeepLifePersistence.load(context, c, u))
                            val loadedLife = loadedDeep.lifeSimulation ?: LifeSimulationDirector.bootstrap(c, loadedDeep)
                            val syncedLife = LifeSimulationDirector.synced(c, loadedDeep, loadedLife)
                            val dl = if (loadedDeep.lifeSimulation != syncedLife) {
                                loadedDeep.copy(lifeSimulation = syncedLife).also {
                                    deep = it
                                    DeepLifePersistence.save(context, it)
                                }
                            } else loadedDeep.also { deep = it }

                            val choiceHandler: (EventNode, Choice) -> Unit = { event, choice ->
                                val current = campaign
                                if (current != null) {
                                    haptic(if (event.stakes >= 4) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD)
                                    MetahumanAudioHooks.onChoice()
                                    val currentState = ultimate ?: UltimateStore.load(context, current)
                                    val currentDeep = deep ?: DeepLifePersistence.load(context, current, currentState)
                                    val result = UltimateGameEngine.resolve(current, currentState, event, choice)
                                    val deepUpdate = DeepLifeRuntime.afterChoice(current, result.campaign, event, choice, currentDeep)
                                    val worldUpdate = DeepWorldDirector.afterChoice(result.campaign, result.state, deepUpdate.state, event, choice)
                                    val generationUpdate = GenerationalDirector.afterChoice(worldUpdate.campaign, worldUpdate.ultimate, worldUpdate.deep, event, choice)
                                    val revealedDeep = DeepLifeDirector.revealPower(generationUpdate.campaign, generationUpdate.deep)
                                    val life = LifeSimulationDirector.synced(
                                        generationUpdate.campaign,
                                        revealedDeep,
                                        revealedDeep.lifeSimulation ?: LifeSimulationDirector.bootstrap(generationUpdate.campaign, revealedDeep)
                                    )
                                    val nextDeep = LifeSimulationDirector.mergedIntoDeep(revealedDeep, life)
                                    val nextAnnual = (annual ?: AnnualActionState.fresh(generationUpdate.campaign)).synced(generationUpdate.campaign)
                                    persist(generationUpdate.campaign, generationUpdate.ultimate, nextAnnual, nextDeep)
                                    val combinedOutcome = buildString {
                                        append(result.outcome)
                                        if (deepUpdate.echo.isNotBlank()) append("\n\nTRACE DE VIE\n${deepUpdate.echo}")
                                        if (worldUpdate.echo.isNotBlank()) append("\n\nMONDE QUI RÉAGIT\n${worldUpdate.echo}")
                                        if (generationUpdate.echo.isNotBlank()) append("\n\nPASSAGE DE RELAIS\n${generationUpdate.echo}")
                                    }
                                    outcome = combinedOutcome
                                    saveRebuildOutcome(context, generationUpdate.campaign.seed, combinedOutcome)
                                }
                            }

                            val actionHandler: (AnnualActionCard) -> AnnualActionResult? = { card ->
                                val current = campaign
                                if (current == null) null else {
                                    haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                    val currentState = ultimate ?: UltimateStore.load(context, current)
                                    val currentDeep = deep ?: DeepLifePersistence.load(context, current, currentState)
                                    val currentAnnual = (annual ?: AnnualActionPersistence.load(context, current)).synced(current)
                                    val action = AnnualActionEngine.perform(current, currentAnnual, card)
                                    if (action == null) null else {
                                        val nextState = UltimateGameEngine.afterAnnualAction(action.campaign, currentState, action.state, card)
                                        val deepUpdate = DeepLifeRuntime.afterAnnualAction(action.campaign, card, currentDeep)
                                        val worldUpdate = DeepWorldDirector.afterAnnualAction(action.campaign, nextState, deepUpdate.state, card)
                                        val lifeBefore = worldUpdate.deep.lifeSimulation ?: LifeSimulationDirector.bootstrap(action.campaign, worldUpdate.deep)
                                        val lifeAfter = LifeSimulationDirector.synced(action.campaign, worldUpdate.deep, lifeBefore).let { life ->
                                            life.copy(civil = life.civil.copy(freeMoments = (life.civil.freeMoments - 1).coerceAtLeast(0)))
                                        }
                                        val nextDeep = LifeSimulationDirector.mergedIntoDeep(worldUpdate.deep, lifeAfter)
                                        persist(worldUpdate.campaign, worldUpdate.ultimate, action.state, nextDeep)
                                        val extra = listOf(deepUpdate.echo, worldUpdate.echo).filter { it.isNotBlank() }.joinToString("\n\n")
                                        action.copy(campaign = worldUpdate.campaign, text = if (extra.isBlank()) action.text else action.text + "\n\n" + extra)
                                    }
                                }
                            }

                            val lifeActionHandler: (LifeAction) -> LifeActionResult? = { lifeAction ->
                                val current = campaign
                                if (current == null) null else {
                                    haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                    val currentState = ultimate ?: UltimateStore.load(context, current)
                                    val currentDeep = deep ?: DeepLifePersistence.load(context, current, currentState)
                                    val baseLife = currentDeep.lifeSimulation ?: LifeSimulationDirector.bootstrap(current, currentDeep)
                                    val synced = LifeSimulationDirector.synced(current, currentDeep, baseLife)
                                    val result = LifeSimulationDirector.perform(current, synced, lifeAction)
                                    if (result.state == synced) result else {
                                        val bridge = LifeWorldStateBridge.afterLifeAction(current, currentState, synced, result.state)
                                        val nextDeep = LifeSimulationDirector.mergedIntoDeep(currentDeep, result.state)
                                        val currentAnnual = (annual ?: AnnualActionPersistence.load(context, current)).synced(bridge.campaign)
                                        val nextAnnual = currentAnnual.copy(used = (currentAnnual.used + 1).coerceAtMost(ANNUAL_ACTION_LIMIT))
                                        persist(bridge.campaign, bridge.ultimate, nextAnnual, nextDeep)
                                        result
                                    }
                                }
                            }

                            when (renderedScreen) {
                                "DESTIN" -> GameplayRebuildEarlyShell(
                                    c = c,
                                    state = u,
                                    annual = a,
                                    deep = dl,
                                    outcome = outcome,
                                    savePulse = savePulse,
                                    onScreen = { go(it) },
                                    onContinue = {
                                        haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                        outcome = null
                                        saveRebuildOutcome(context, c.seed, null)
                                        if (campaign?.needsAlias == true) screen = "ALIAS"
                                    },
                                    onChoice = choiceHandler,
                                    onHome = { go("HOME") },
                                    onSettings = { go("SETTINGS") }
                                )

                                "VILLE" -> GameplayRebuildCityScreen(c, u, dl) { go("DESTIN") }

                                "LIENS" -> GameplayRebuildLinksScreen(c, a, dl, actionHandler) { go("DESTIN") }

                                "ACTIONS" -> GameplayRebuildActionsHub(c, u, a, dl, actionHandler, lifeActionHandler) { go("DESTIN") }

                                "PERSONNAGE" -> GameplayRebuildCharacterScreen(
                                    c = c,
                                    state = u,
                                    deep = dl,
                                    onStateChange = { next ->
                                        ultimate = next
                                        UltimateStore.save(context, next)
                                        DeepLifePersistence.save(context, dl)
                                        savePulse++
                                        saveCampaignV4(context, c)
                                    },
                                    onBack = { go("DESTIN") }
                                )

                                else -> GameplayRebuildEarlyShell(
                                    c = c,
                                    state = u,
                                    annual = a,
                                    deep = dl,
                                    outcome = outcome,
                                    savePulse = savePulse,
                                    onScreen = { go(it) },
                                    onContinue = {
                                        haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                        outcome = null
                                        saveRebuildOutcome(context, c.seed, null)
                                        if (campaign?.needsAlias == true) screen = "ALIAS"
                                    },
                                    onChoice = choiceHandler,
                                    onHome = { go("HOME") },
                                    onSettings = { go("SETTINGS") }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
