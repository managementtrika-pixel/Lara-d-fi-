package com.metahumanlegacy.game

import android.content.Context
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
            var screen by remember { mutableStateOf("HOME") }
            var hall by remember { mutableStateOf(loadHallV4(context)) }
            var outcome by remember { mutableStateOf(loadUltimateOutcome(context, campaign?.seed)) }
            var draftSeed by remember { mutableStateOf(System.currentTimeMillis()) }
            var blueprint by remember { mutableStateOf(GameEngine.randomBlueprint(draftSeed)) }
            var draft by remember { mutableStateOf(UltimateCatalog.randomDraft(draftSeed, blueprint)) }
            var savePulse by remember { mutableIntStateOf(0) }

            fun persist(c: Campaign, u: UltimateState, a: AnnualActionState? = annual) {
                campaign = c
                ultimate = u
                saveCampaignV4(context, c)
                UltimateStore.save(context, u)
                if (a != null) {
                    annual = a
                    AnnualActionPersistence.save(context, a)
                }
                savePulse++
            }

            fun newDraft() {
                draftSeed = System.currentTimeMillis() xor 0x5A17L
                blueprint = GameEngine.randomBlueprint(draftSeed)
                draft = UltimateCatalog.randomDraft(draftSeed, blueprint)
            }

            fun startLife(d: UltimateCreationDraft) {
                val seed = System.currentTimeMillis()
                val c = GameEngine.newCampaign(seed, d.blueprint)
                val u = UltimateStore.create(c, d)
                val a = AnnualActionState.fresh(c)
                campaign = c; ultimate = u; annual = a
                saveCampaignV4(context, c)
                UltimateStore.save(context, u)
                AnnualActionPersistence.save(context, a)
                outcome = null
                saveUltimateOutcome(context, seed, null)
                savePulse++
                screen = "DESTIN"
            }

            fun abandon() {
                campaign?.let {
                    UltimateStore.clear(context, it.seed)
                    AnnualActionPersistence.clear(context, it.seed)
                    saveUltimateOutcome(context, it.seed, null)
                }
                clearCampaignV4(context)
                campaign = null; ultimate = null; annual = null; outcome = null
                newDraft()
                screen = "CREATE"
            }

            UltimateRootBackdrop(
                campaign = campaign,
                state = ultimate,
                scene = if (campaign?.finished == true) "LEGACY" else screen
            ) {
                when {
                    screen == "SETTINGS" -> UltimateSettingsScreen(
                        settings = motion,
                        onChange = controller.update,
                        onBack = { screen = if (campaign == null) "HOME" else "DESTIN" }
                    )

                    screen == "HALL" -> UltimateHallScreen(hall) { screen = "HOME" }

                    screen == "HOME" -> UltimateHomeScreen(
                        campaign = campaign,
                        state = ultimate,
                        hallCount = hall.size,
                        onContinue = { screen = if (campaign?.needsAlias == true) "ALIAS" else "DESTIN" },
                        onNew = { abandon() },
                        onHall = { screen = "HALL" },
                        onSettings = { screen = "SETTINGS" }
                    )

                    campaign == null && screen == "CREATE" -> UltimateCreateScreen(
                        draft = draft,
                        onDraft = {
                            draft = it
                            blueprint = it.blueprint
                        },
                        onRandomize = {
                            newDraft()
                        },
                        onBack = { screen = "HOME" },
                        onStart = { startLife(it) }
                    )

                    campaign == null -> UltimateHomeScreen(
                        campaign = null,
                        state = null,
                        hallCount = hall.size,
                        onContinue = { },
                        onNew = { screen = "CREATE" },
                        onHall = { screen = "HALL" },
                        onSettings = { screen = "SETTINGS" }
                    )

                    campaign!!.finished -> UltimateFinalScreen(campaign!!, ultimate ?: UltimateStore.fallback(campaign!!)) {
                        val c = campaign!!
                        val u = ultimate ?: UltimateStore.fallback(c)
                        val who = c.alias.ifBlank { c.name }
                        val entry = listOf(
                            who,
                            GameEngine.legacyTitle(c),
                            GameEngine.legacyScore(c),
                            c.scope.label,
                            c.city,
                            u.heroPresentation,
                            u.nemesis.ifBlank { "Aucune" }
                        ).joinToString("|")
                        hall = (listOf(entry) + hall).distinct().take(60)
                        saveHallV4(context, hall)
                        UltimateStore.clear(context, c.seed)
                        AnnualActionPersistence.clear(context, c.seed)
                        saveUltimateOutcome(context, c.seed, null)
                        clearCampaignV4(context)
                        campaign = null; ultimate = null; annual = null; outcome = null
                        newDraft()
                        screen = "HOME"
                    }

                    screen == "ALIAS" -> UltimateAliasScreen(campaign!!, ultimate ?: UltimateStore.fallback(campaign!!)) { alias, presentation, palette, mask ->
                        val c = GameEngine.setAlias(campaign!!, alias)
                        val u = (ultimate ?: UltimateStore.fallback(c)).copy(
                            heroPresentation = presentation,
                            costumePalette = palette,
                            maskStyle = mask,
                            costumeEra = maxOf(1, (ultimate ?: UltimateStore.fallback(c)).costumeEra)
                        )
                        persist(c, u)
                        screen = "DESTIN"
                    }

                    else -> {
                        val c = campaign!!
                        val u = ultimate ?: UltimateStore.fallback(c).also { ultimate = it }
                        val a = (annual ?: AnnualActionPersistence.load(context, c)).synced(c).also { annual = it }
                        UltimateCareerShell(
                            c = c,
                            state = u,
                            annual = a,
                            screen = screen,
                            outcome = outcome,
                            savePulse = savePulse,
                            onScreen = { screen = it },
                            onContinue = {
                                outcome = null
                                saveUltimateOutcome(context, c.seed, null)
                                if (campaign?.needsAlias == true) screen = "ALIAS"
                            },
                            onChoice = { event, choice ->
                                val current = campaign ?: return@UltimateCareerShell
                                val currentState = ultimate ?: UltimateStore.load(context, current)
                                val result = UltimateGameEngine.resolve(current, currentState, event, choice)
                                val nextAnnual = (annual ?: AnnualActionState.fresh(result.campaign)).synced(result.campaign)
                                persist(result.campaign, result.state, nextAnnual)
                                outcome = result.outcome
                                saveUltimateOutcome(context, result.campaign.seed, result.outcome)
                            },
                            onAction = { card ->
                                val current = campaign ?: return@UltimateCareerShell null
                                val currentState = ultimate ?: UltimateStore.load(context, current)
                                val currentAnnual = (annual ?: AnnualActionPersistence.load(context, current)).synced(current)
                                val action = AnnualActionEngine.perform(current, currentAnnual, card) ?: return@UltimateCareerShell null
                                val nextState = UltimateGameEngine.afterAnnualAction(action.campaign, currentState, action.state, card)
                                persist(action.campaign, nextState, action.state)
                                action.copy(campaign = action.campaign)
                            },
                            onStateChange = { next ->
                                val current = campaign ?: return@UltimateCareerShell
                                ultimate = next
                                UltimateStore.save(context, next)
                                savePulse++
                                saveCampaignV4(context, current)
                            },
                            onHome = { screen = "HOME" },
                            onSettings = { screen = "SETTINGS" },
                            onRestart = { abandon() }
                        )
                    }
                }
            }
        }
    }
}
