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

private const val ULTIMATE_SESSION = "mhl_ultimate_session_v1"

private fun loadUltimateOutcome(context: Context, seed: Long?): String? {
    if (seed == null) return null
    return context.getSharedPreferences(ULTIMATE_SESSION, Context.MODE_PRIVATE).getString("outcome_$seed", null)
}

private fun saveUltimateOutcome(context: Context, seed: Long, value: String?) {
    val edit = context.getSharedPreferences(ULTIMATE_SESSION, Context.MODE_PRIVATE).edit()
    if (value == null) edit.remove("outcome_$seed") else edit.putString("outcome_$seed", value)
    edit.apply()
}

@Composable
fun UltimateMetahumanLegacyApp(context: Context) {
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
            var outcome by remember { mutableStateOf(loadUltimateOutcome(context, campaign?.seed)) }
            var draftSeed by remember { mutableStateOf(System.currentTimeMillis()) }
            var blueprint by remember { mutableStateOf(GameEngine.randomBlueprint(draftSeed)) }
            var draft by remember { mutableStateOf(UltimateCatalog.randomDraft(draftSeed, blueprint)) }
            var savePulse by remember { mutableIntStateOf(0) }

            fun persist(
                c: Campaign,
                u: UltimateState,
                a: AnnualActionState? = annual,
                d: DeepLifeState? = deep
            ) {
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
                val dl = DeepLifeDirector.bootstrap(c, u)
                campaign = c; ultimate = u; annual = a; deep = dl
                saveCampaignV4(context, c)
                UltimateStore.save(context, u)
                AnnualActionPersistence.save(context, a)
                DeepLifePersistence.save(context, dl)
                outcome = null
                saveUltimateOutcome(context, seed, null)
                savePulse++
                haptic(MetahumanMotionLevel.MOTION_STANDARD)
                screen = "DESTIN"
            }

            fun abandon() {
                campaign?.let {
                    UltimateStore.clear(context, it.seed)
                    AnnualActionPersistence.clear(context, it.seed)
                    DeepLifePersistence.clear(context, it.seed)
                    saveUltimateOutcome(context, it.seed, null)
                }
                clearCampaignV4(context)
                campaign = null; ultimate = null; annual = null; deep = null; outcome = null
                newDraft()
                screen = "CREATE"
            }

            UltimateRootBackdrop(
                campaign = campaign,
                state = ultimate,
                scene = if (campaign?.finished == true) "LEGACY" else screen
            ) {
                val transitionDuration = MetahumanMotionTokens.duration(MetahumanMotionTokens.FAST, motion)
                val stageKey = "${screen}|${campaign?.turn ?: -1}|${outcome?.hashCode() ?: 0}|${campaign?.needsAlias == true}"
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
                    label = "ultimate-stage-transition"
                ) { targetStage ->
                    val renderedScreen = targetStage.substringBefore('|')
                    when {
                        renderedScreen == "SETTINGS" -> UltimateSettingsScreen(
                            settings = motion,
                            onChange = controller.update,
                            onBack = { go(if (campaign == null) "HOME" else "DESTIN") }
                        )

                        renderedScreen == "HALL" -> UltimateHallScreen(hall) { go("HOME") }

                        renderedScreen == "HOME" -> UltimateHomeScreen(
                            campaign = campaign,
                            state = ultimate,
                            hallCount = hall.size,
                            onContinue = { go(if (campaign?.needsAlias == true) "ALIAS" else "DESTIN", MetahumanMotionLevel.MOTION_STANDARD) },
                            onNew = { abandon() },
                            onHall = { go("HALL") },
                            onSettings = { go("SETTINGS") }
                        )

                        campaign == null && renderedScreen == "CREATE" -> UltimateCreateScreen(
                            draft = draft,
                            onDraft = {
                                draft = it
                                blueprint = it.blueprint
                            },
                            onRandomize = {
                                haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                newDraft()
                            },
                            onBack = { go("HOME") },
                            onStart = { startLife(it) }
                        )

                        campaign == null -> UltimateHomeScreen(
                            campaign = null,
                            state = null,
                            hallCount = hall.size,
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
                            hall = (listOf(entry) + hall)
                                .distinctBy { LegacyRecord.decode(it).identityId.ifBlank { LegacyRecord.decode(it).name + "|" + LegacyRecord.decode(it).title } }
                                .take(60)
                            saveHallV4(context, hall)
                            UltimateStore.clear(context, c.seed)
                            AnnualActionPersistence.clear(context, c.seed)
                            DeepLifePersistence.clear(context, c.seed)
                            saveUltimateOutcome(context, c.seed, null)
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

                        else -> {
                            val c = campaign!!
                            val u = ultimate ?: UltimateStore.fallback(c).also { ultimate = it }
                            val a = (annual ?: AnnualActionPersistence.load(context, c)).synced(c).also { annual = it }
                            (deep ?: DeepLifePersistence.load(context, c, u)).also { deep = it }
                            UltimateCareerShell(
                                c = c,
                                state = u,
                                annual = a,
                                screen = renderedScreen,
                                outcome = outcome,
                                savePulse = savePulse,
                                onScreen = { go(it) },
                                onContinue = {
                                    haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                    outcome = null
                                    saveUltimateOutcome(context, c.seed, null)
                                    if (campaign?.needsAlias == true) screen = "ALIAS"
                                },
                                onChoice = { event, choice ->
                                    val current = campaign ?: return@UltimateCareerShell
                                    haptic(if (event.stakes >= 4) MetahumanMotionLevel.MOTION_MAJOR else MetahumanMotionLevel.MOTION_STANDARD)
                                    MetahumanAudioHooks.onChoice()
                                    val currentState = ultimate ?: UltimateStore.load(context, current)
                                    val currentDeep = deep ?: DeepLifePersistence.load(context, current, currentState)
                                    val result = UltimateGameEngine.resolve(current, currentState, event, choice)
                                    val deepUpdate = DeepLifeRuntime.afterChoice(current, result.campaign, event, choice, currentDeep)
                                    val worldUpdate = DeepWorldDirector.afterChoice(result.campaign, result.state, deepUpdate.state, event, choice)
                                    val nextDeep = DeepLifeDirector.revealPower(worldUpdate.campaign, worldUpdate.deep)
                                    val nextAnnual = (annual ?: AnnualActionState.fresh(worldUpdate.campaign)).synced(worldUpdate.campaign)
                                    persist(worldUpdate.campaign, worldUpdate.ultimate, nextAnnual, nextDeep)
                                    val combinedOutcome = buildString {
                                        append(result.outcome)
                                        if (deepUpdate.echo.isNotBlank()) {
                                            append("\n\nTRACE DE VIE\n")
                                            append(deepUpdate.echo)
                                        }
                                        if (worldUpdate.echo.isNotBlank()) {
                                            append("\n\nMONDE QUI RÉAGIT\n")
                                            append(worldUpdate.echo)
                                        }
                                        if (worldUpdate.campaign.powerRevealed && event.kind != "FORMATIVE") {
                                            append("\n\nPERCEPTIONS\n")
                                            append(DeepLifeRuntime.perceptionSummary(nextDeep))
                                        }
                                    }
                                    outcome = combinedOutcome
                                    saveUltimateOutcome(context, worldUpdate.campaign.seed, combinedOutcome)
                                },
                                onAction = { card ->
                                    val current = campaign ?: return@UltimateCareerShell null
                                    haptic(MetahumanMotionLevel.MOTION_SUBTLE)
                                    val currentState = ultimate ?: UltimateStore.load(context, current)
                                    val currentDeep = deep ?: DeepLifePersistence.load(context, current, currentState)
                                    val currentAnnual = (annual ?: AnnualActionPersistence.load(context, current)).synced(current)
                                    val action = AnnualActionEngine.perform(current, currentAnnual, card) ?: return@UltimateCareerShell null
                                    val nextState = UltimateGameEngine.afterAnnualAction(action.campaign, currentState, action.state, card)
                                    val deepUpdate = DeepLifeRuntime.afterAnnualAction(action.campaign, card, currentDeep)
                                    val worldUpdate = DeepWorldDirector.afterAnnualAction(action.campaign, nextState, deepUpdate.state, card)
                                    persist(worldUpdate.campaign, worldUpdate.ultimate, action.state, worldUpdate.deep)
                                    val extra = listOf(deepUpdate.echo, worldUpdate.echo).filter { it.isNotBlank() }.joinToString("\n\n")
                                    action.copy(
                                        campaign = worldUpdate.campaign,
                                        text = if (extra.isBlank()) action.text else action.text + "\n\n" + extra
                                    )
                                },
                                onStateChange = { next ->
                                    val current = campaign ?: return@UltimateCareerShell
                                    ultimate = next
                                    UltimateStore.save(context, next)
                                    deep?.let { DeepLifePersistence.save(context, it) }
                                    savePulse++
                                    saveCampaignV4(context, current)
                                },
                                onHome = { go("HOME") },
                                onSettings = { go("SETTINGS") },
                                onRestart = { abandon() }
                            )
                        }
                    }
                }
            }
        }
    }
}
