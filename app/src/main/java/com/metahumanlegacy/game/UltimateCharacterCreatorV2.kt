package com.metahumanlegacy.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CreatorStep(val number: String, val title: String, val subtitle: String)

private val creatorSteps = listOf(
    CreatorStep("01", "IDENTITÉ", "Chaque légende commence par une personne."),
    CreatorStep("02", "VISAGE", "Choisis une apparence forte ou crée la tienne."),
    CreatorStep("03", "CORPS", "Construis une silhouette immédiatement reconnaissable."),
    CreatorStep("04", "CHEVEUX", "Coiffure, pilosité et regard façonnent la présence."),
    CreatorStep("05", "STYLE", "Avant le costume, il y a une façon d'habiter le monde."),
    CreatorStep("06", "DÉTAILS", "Les petits signes font une identité."),
    CreatorStep("07", "VILLE", "Ton histoire commence quelque part."),
    CreatorStep("08", "VALIDATION", "Avant le pouvoir, il y avait une personne.")
)

@Composable
internal fun UltimateCharacterCreatorV2(
    draft: UltimateCreationDraft,
    onDraft: (UltimateCreationDraft) -> Unit,
    onRandomize: () -> Unit,
    onBack: () -> Unit,
    onStart: (UltimateCreationDraft) -> Unit
) {
    var step by remember(draft.blueprint.fullName) { mutableIntStateOf(0) }
    fun updateBlueprint(next: CharacterBlueprint) = onDraft(draft.copy(blueprint = next))
    val previewCampaign = remember(draft) { GameEngine.newCampaign(10101L, draft.blueprint) }
    val previewState = remember(draft) { UltimateStore.create(previewCampaign, draft) }
    val meta = creatorSteps[step]
    val canAdvance = draft.blueprint.fullName.isNotBlank()

    MhlSceneFrame(
        "creator-v2-" + step + "-" + draft.hashCode(),
        MotionBoard.PANEL_TRANSITION,
        MetahumanMotionLevel.MOTION_STANDARD,
        Modifier.fillMaxSize(),
        UltimateBlue
    ) {
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(listOf(Color(0xFF05080D), Color(0xFF0A1018), Color(0xFF05080D)))
            )
        ) {
            CreatorBackdrop()

            Column(Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 10.dp)) {
                CreatorTopBar(step, creatorSteps.size, meta.number, meta.title, meta.subtitle, onRandomize)
                Spacer(Modifier.height(8.dp))
                AnimatedContent(
                    targetState = step,
                    transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(140)) },
                    modifier = Modifier.weight(1f),
                    label = "creatorStep"
                ) { current ->
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                        when (current) {
                            0 -> IdentityStep(draft, previewCampaign, previewState, ::updateBlueprint)
                            1 -> FaceStep(draft, previewCampaign, previewState, onDraft)
                            2 -> BodyStep(draft, previewCampaign, previewState, onDraft)
                            3 -> HairStep(draft, previewCampaign, previewState, onDraft)
                            4 -> StyleStep(draft, previewCampaign, previewState, onDraft)
                            5 -> DetailsStep(draft, previewCampaign, previewState, ::updateBlueprint)
                            6 -> CityStep(draft, previewCampaign, previewState, onDraft, ::updateBlueprint)
                            else -> ValidationStep(draft, previewCampaign, previewState)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MhlSecondaryButton(
                        if (step == 0) "Retour" else "Précédent",
                        { if (step == 0) onBack() else step-- },
                        Modifier.weight(1f)
                    )
                    MhlPrimaryButton(
                        if (step == creatorSteps.lastIndex) "COMMENCER L'AVENTURE" else "Suivant",
                        { if (step == creatorSteps.lastIndex) onStart(draft) else step++ },
                        Modifier.weight(1f),
                        canAdvance
                    )
                }
            }
        }
    }
}

@Composable
private fun CreatorBackdrop() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.align(Alignment.TopEnd)
                .offset(x = 70.dp, y = (-45).dp)
                .size(210.dp)
                .background(UltimateBlue.copy(alpha = .035f), RoundedCornerShape(105.dp))
        )
        Box(
            Modifier.align(Alignment.BottomStart)
                .offset(x = (-80).dp, y = 70.dp)
                .size(240.dp)
                .background(UltimateGold.copy(alpha = .025f), RoundedCornerShape(120.dp))
        )
    }
}


@Composable
private fun CreatorTopBar(
    step: Int,
    total: Int,
    number: String,
    title: String,
    subtitle: String,
    onRandomize: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Box(
            Modifier.background(UltimateIvory, CutCornerShape(topEnd = 10.dp, bottomStart = 10.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Text(number, color = Color(0xFF090D12), fontWeight = FontWeight.Black, fontSize = 16.sp)
        }
        Spacer(Modifier.width(9.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 25.sp, letterSpacing = .8.sp)
            Text(subtitle, color = UltimateMuted, fontSize = 10.sp, lineHeight = 14.sp)
        }
        TextButton(onClick = onRandomize) {
            Text("ALÉATOIRE", color = UltimateBlue, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
    Spacer(Modifier.height(7.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(total) { i ->
            Box(
                Modifier.weight(1f).height(if (i == step) 5.dp else 3.dp).background(
                    when {
                        i == step -> UltimateBlue
                        i < step -> UltimateGold
                        else -> Color(0xFF232C37)
                    }
                )
            )
        }
    }
}

@Composable
private fun CharacterStage(campaign: Campaign, state: UltimateState, caption: String) {
    Box(
        Modifier.fillMaxWidth().height(350.dp)
            .shadow(18.dp, CutCornerShape(topEnd = 30.dp, bottomStart = 30.dp))
            .clip(CutCornerShape(topEnd = 30.dp, bottomStart = 30.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF152231), Color(0xFF080C12))))
            .border(1.dp, UltimateBlue.copy(alpha = .32f), CutCornerShape(topEnd = 30.dp, bottomStart = 30.dp))
    ) {
        Box(
            Modifier.align(Alignment.Center)
                .width(260.dp).height(260.dp)
                .background(
                    Brush.radialGradient(
                        listOf(UltimateBlue.copy(alpha = .18f), Color.Transparent)
                    )
                )
        )

        UltimatePortrait(
            campaign,
            state,
            Modifier.align(Alignment.Center).width(255.dp).height(335.dp),
            heroMode = false,
            showAura = false
        )
        Box(
            Modifier.align(Alignment.BottomStart).fillMaxWidth()
                .background(Color.Black.copy(alpha = .48f))
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(caption.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 10.sp)
        }
    }
}

@Composable
private fun IdentityStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    updateBlueprint: (CharacterBlueprint) -> Unit
) {
    CharacterStage(campaign, state, "Qui seras-tu avant l'éveil ?")
    Spacer(Modifier.height(10.dp))
    UltimatePanel(accent = UltimateBlue) {
        OutlinedTextField(
            draft.blueprint.firstName,
            { updateBlueprint(draft.blueprint.copy(firstName = it.take(24))) },
            label = { Text("Prénom") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(7.dp))
        OutlinedTextField(
            draft.blueprint.lastName,
            { updateBlueprint(draft.blueprint.copy(lastName = it.take(24))) },
            label = { Text("Nom") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
    }
    CreatorOptionStrip("PRONOM", GameEngine.pronouns, draft.blueprint.pronouns) {
        updateBlueprint(draft.blueprint.copy(pronouns = it))
    }
}

@Composable
private fun FaceStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit
) {
    CharacterStage(campaign, state, if (draft.libraryFaceIndex >= 0) "Identité illustrée" else "Création libre")
    LibraryFacePresetStrip(draft, onDraft)
    Spacer(Modifier.height(9.dp))
    if (draft.libraryFaceIndex >= 0) {
        UltimatePanel(accent = UltimateGold) {
            Text("APPARENCE PRÉDÉFINIE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text(
                "Le portrait reste une identité complète pour garantir un rendu propre et cohérent.",
                color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
            Spacer(Modifier.height(8.dp))
            MhlSecondaryButton("CRÉER LIBREMENT", { onDraft(draft.copy(libraryFaceIndex = -1)) }, Modifier.fillMaxWidth())
        }
    } else {
        UltimatePanel(accent = UltimateBlue) {
            Text("CRÉATION LIBRE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text("Les réglages sont gérés par le renderer du jeu.", color = UltimateMuted, fontSize = 11.sp)
        }
        CreatorOptionStrip("TEINT", UltimateCatalog.skinTones, draft.skinTone) { onDraft(draft.copy(skinTone = it)) }
        CreatorOptionStrip("FORME DU VISAGE", UltimateCatalog.faceShapes, draft.faceShape) { onDraft(draft.copy(faceShape = it)) }
    }
}

@Composable
private fun BodyStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit
) {
    CharacterStage(campaign, state, draft.bodyBuild + " · " + draft.stature)
    CreatorOptionStrip("SILHOUETTE", UltimateCatalog.bodyBuilds, draft.bodyBuild) { onDraft(draft.copy(bodyBuild = it)) }
    CreatorOptionStrip("TAILLE", UltimateCatalog.statures, draft.stature) { onDraft(draft.copy(stature = it)) }
}

@Composable
private fun HairStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit
) {
    CharacterStage(campaign, state, if (draft.libraryFaceIndex >= 0) "Cheveux intégrés au portrait" else draft.hair)
    if (draft.libraryFaceIndex >= 0) {
        UltimatePanel(accent = UltimateGold) {
            Text("IDENTITÉ ILLUSTRÉE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp)
            Text(
                "Coiffure, yeux et pilosité appartiennent au portrait choisi afin d'éviter les superpositions incohérentes.",
                color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    } else {
        CreatorOptionStrip("COIFFURE", UltimateCatalog.hairs, draft.hair) { onDraft(draft.copy(hair = it)) }
        CreatorOptionStrip("COULEUR", UltimateCatalog.hairColors, draft.hairColor) { onDraft(draft.copy(hairColor = it)) }
        CreatorOptionStrip("BARBE", UltimateCatalog.facialHairs, draft.facialHair) { onDraft(draft.copy(facialHair = it)) }
        CreatorOptionStrip("YEUX", UltimateCatalog.eyes, draft.eyes) { onDraft(draft.copy(eyes = it)) }
    }
}

@Composable
private fun StyleStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit
) {
    CharacterStage(campaign, state, draft.civilianStyle)
    CreatorOptionStrip("STYLE CIVIL", UltimateCatalog.civilianStyles, draft.civilianStyle) { onDraft(draft.copy(civilianStyle = it)) }
    CreatorOptionStrip("ACCESSOIRE", UltimateCatalog.accessories, draft.accessory) { onDraft(draft.copy(accessory = it)) }
}

@Composable
private fun DetailsStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    updateBlueprint: (CharacterBlueprint) -> Unit
) {
    CharacterStage(campaign, state, "Ce qui te rend reconnaissable")
    CreatorOptionStrip("CONTEXTE SOCIAL", GameEngine.socialBackgrounds, draft.blueprint.socialBackground) {
        updateBlueprint(draft.blueprint.copy(socialBackground = it))
    }
    CreatorOptionStrip("TRAJECTOIRE", GameEngine.civilianPaths, draft.blueprint.civilianPath) {
        updateBlueprint(draft.blueprint.copy(civilianPath = it))
    }
    CreatorOptionStrip("MOTIVATION", GameEngine.motivations, draft.blueprint.motivation) {
        updateBlueprint(draft.blueprint.copy(motivation = it))
    }
    CreatorOptionStrip("TEMPÉRAMENT", GameEngine.temperaments, draft.blueprint.temperament) {
        updateBlueprint(draft.blueprint.copy(temperament = it))
    }
}

@Composable
private fun CityStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit,
    updateBlueprint: (CharacterBlueprint) -> Unit
) {
    UltimateCityArtwork(
        campaign,
        state,
        Modifier.fillMaxWidth().height(260.dp).clip(CutCornerShape(topEnd = 24.dp, bottomStart = 24.dp))
    )
    Spacer(Modifier.height(8.dp))
    LibraryCityPresetStrip(draft, onDraft)
    CreatorOptionStrip("VILLE", GameEngine.cities, draft.blueprint.city) { updateBlueprint(draft.blueprint.copy(city = it)) }
    CreatorOptionStrip("QUARTIER", GameEngine.districts, draft.blueprint.district) { updateBlueprint(draft.blueprint.copy(district = it)) }
    CreatorOptionStrip("TYPE DE VILLE", UltimateCatalog.cityArchetypes, draft.cityArchetype) { onDraft(draft.copy(cityArchetype = it)) }
    CreatorOptionStrip("CLIMAT", UltimateCatalog.climates, draft.climate) { onDraft(draft.copy(climate = it)) }
    CreatorOptionStrip("ARCHITECTURE", UltimateCatalog.architectures, draft.architecture) { onDraft(draft.copy(architecture = it)) }
    CreatorOptionStrip("AMBIANCE", UltimateCatalog.cityMoods, draft.cityMood) { onDraft(draft.copy(cityMood = it)) }
}

@Composable
private fun ValidationStep(draft: UltimateCreationDraft, campaign: Campaign, state: UltimateState) {
    CharacterStage(campaign, state, "Avant le pouvoir, il y avait une personne.")
    Spacer(Modifier.height(10.dp))
    UltimatePanel(accent = UltimateGold) {
        Text("DOSSIER D'IDENTITÉ", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 9.sp, letterSpacing = 1.sp)
        Text(draft.blueprint.fullName.uppercase(), color = UltimateIvory, fontWeight = FontWeight.Black, fontSize = 22.sp)
        Spacer(Modifier.height(5.dp))
        Text(
            draft.bodyBuild + " · " + draft.stature + "\n" +
                draft.civilianStyle + " · " + draft.accessory + "\n" +
                draft.blueprint.city + ", " + draft.blueprint.district + "\n" +
                draft.cityArchetype + " · " + draft.cityMood + "\n" +
                draft.blueprint.socialBackground + " · " + draft.blueprint.civilianPath + "\n" +
                draft.blueprint.motivation + " · " + draft.blueprint.temperament,
            color = UltimateMuted, fontSize = 11.sp, lineHeight = 17.sp
        )
    }
    Spacer(Modifier.height(8.dp))
    UltimatePanel(accent = UltimateBlue) {
        Text("PREMIÈRE ANNÉE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
        Text(
            "Tu ne choisis toujours aucun pouvoir. Tes dix premières décisions construiront secrètement l'éveil.",
            color = UltimateIvory, fontWeight = FontWeight.Bold, fontSize = 12.sp, lineHeight = 18.sp
        )
    }
}

@Composable
private fun CreatorOptionStrip(title: String, options: List<String>, selected: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(13.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(Modifier.weight(1f))
        Text(selected.uppercase(), color = UltimateBlue, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            Box(
                Modifier
                    .widthIn(min = 108.dp, max = 180.dp)
                    .heightIn(min = 52.dp)
                    .clip(CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp))
                    .background(
                        if (isSelected) {
                            Brush.horizontalGradient(
                                listOf(UltimateBlue.copy(alpha = .24f), UltimateGold.copy(alpha = .12f))
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color(0xFF111821), Color(0xFF0B1118)))
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) UltimateBlue.copy(alpha = .75f) else Color(0xFF293544),
                        CutCornerShape(topEnd = 14.dp, bottomStart = 14.dp)
                    )
                    .clickable { onSelect(option) }
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(
                        option.uppercase(),
                        color = if (isSelected) UltimateIvory else Color(0xFFCBD4DE),
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        maxLines = 2
                    )
                    if (isSelected) {
                        Spacer(Modifier.height(4.dp))
                        Box(Modifier.width(26.dp).height(2.dp).background(UltimateGold))
                    }
                }
            }
        }
    }
}
