package com.metahumanlegacy.game

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class CreatorStep(val number: String, val title: String, val subtitle: String)

private val creatorSteps = listOf(
    CreatorStep("01", "IDENTITÉ", "Chaque légende commence par une personne."),
    CreatorStep("02", "VISAGE", "Assemble ton visage pixel en quelques choix."),
    CreatorStep("03", "CORPS", "Choisis une silhouette simple et lisible."),
    CreatorStep("04", "CHEVEUX", "Change de coiffure et de look en un tap."),
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
    LaunchedEffect(Unit) { if (draft.libraryFaceIndex >= 0) onDraft(draft.copy(libraryFaceIndex = -1)) }
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

        PixelAvatar(
            state = state,
            modifier = Modifier.align(Alignment.Center).width(255.dp).height(335.dp)
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
    CharacterStage(campaign, state, "Avatar pixel")
    UltimatePanel(accent = UltimateBlue) {
        Text("100 % SANS ASSET", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 9.sp)
        Text(
            "Ton personnage est dessiné directement par le jeu. Chaque option s'assemble proprement et instantanément.",
            color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp
        )
    }
    PixelColorStrip("TEINT", UltimateCatalog.skinTones, draft.skinTone, ::pixelSkinPreview) {
        onDraft(draft.copy(skinTone = it, libraryFaceIndex = -1))
    }
    PixelFaceShapeStrip(draft.faceShape, draft.skinTone) {
        onDraft(draft.copy(faceShape = it, libraryFaceIndex = -1))
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
    PixelBodyShapeStrip(draft.bodyBuild, draft.civilianStyle) { onDraft(draft.copy(bodyBuild = it)) }
    CreatorOptionStrip("TAILLE", UltimateCatalog.statures, draft.stature) { onDraft(draft.copy(stature = it)) }
}

@Composable
private fun HairStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit
) {
    CharacterStage(campaign, state, draft.hair)
    PixelHairStrip(draft.hair, draft.hairColor, draft.skinTone) {
        onDraft(draft.copy(hair = it, libraryFaceIndex = -1))
    }
    PixelColorStrip("COULEUR", UltimateCatalog.hairColors, draft.hairColor, ::pixelHairPreview) {
        onDraft(draft.copy(hairColor = it, libraryFaceIndex = -1))
    }
    CreatorOptionStrip("BARBE", UltimateCatalog.facialHairs, draft.facialHair) { onDraft(draft.copy(facialHair = it, libraryFaceIndex = -1)) }
    CreatorOptionStrip("YEUX", UltimateCatalog.eyes, draft.eyes) { onDraft(draft.copy(eyes = it, libraryFaceIndex = -1)) }
}

@Composable
private fun StyleStep(
    draft: UltimateCreationDraft,
    campaign: Campaign,
    state: UltimateState,
    onDraft: (UltimateCreationDraft) -> Unit
) {
    CharacterStage(campaign, state, draft.civilianStyle)
    PixelStyleStrip(draft.civilianStyle) { onDraft(draft.copy(civilianStyle = it)) }
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

private fun pixelSkinPreview(name: String): Color = when (name) {
    "Très clair" -> Color(0xFFF3D2BD)
    "Clair" -> Color(0xFFE8BFA4)
    "Moyen" -> Color(0xFFC88E67)
    "Mat" -> Color(0xFFAA714F)
    "Foncé" -> Color(0xFF794A34)
    "Très foncé" -> Color(0xFF4C2B22)
    else -> Color(0xFFC88E67)
}

private fun pixelHairPreview(name: String): Color = when (name) {
    "Noir" -> Color(0xFF111318)
    "Brun" -> Color(0xFF3B261E)
    "Châtain" -> Color(0xFF6A4630)
    "Blond" -> Color(0xFFD4B06A)
    "Roux" -> Color(0xFFA9502B)
    "Gris" -> Color(0xFF9699A0)
    "Blanc" -> Color(0xFFE8E5DE)
    else -> Color(0xFF3B261E)
}

@Composable
private fun PixelColorStrip(
    title: String,
    options: List<String>,
    selected: String,
    colorOf: (String) -> Color,
    onSelect: (String) -> Unit
) {
    Spacer(Modifier.height(13.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
        Spacer(Modifier.weight(1f))
        Text(selected.uppercase(), color = UltimateBlue, fontSize = 8.sp, fontWeight = FontWeight.Black)
    }
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        options.forEach { option ->
            val isSelected = selected == option
            Column(
                Modifier.width(72.dp).clickable { onSelect(option) },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    Modifier.size(if (isSelected) 48.dp else 42.dp)
                        .clip(CircleShape)
                        .background(colorOf(option))
                        .border(if (isSelected) 3.dp else 1.dp, if (isSelected) UltimateGold else Color(0xFF394656), CircleShape)
                )
                Spacer(Modifier.height(5.dp))
                Text(
                    option.uppercase(),
                    color = if (isSelected) UltimateIvory else UltimateMuted,
                    fontSize = 7.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun PixelFaceShapeStrip(selected: String, skinTone: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(13.dp))
    Text("FORME DU VISAGE", color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        UltimateCatalog.faceShapes.forEach { shape ->
            val active = shape == selected
            Column(
                Modifier.width(86.dp)
                    .clip(CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .background(if (active) Color(0xFF142334) else Color(0xFF0D141C))
                    .border(1.dp, if (active) UltimateBlue else Color(0xFF2C3948), CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .clickable { onSelect(shape) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.size(54.dp)) {
                    val skin = pixelSkinPreview(skinTone)
                    val outline = Color(0xFF11161C)
                    val w = when (shape) {
                        "Fin" -> size.width * .52f
                        "Rond" -> size.width * .74f
                        else -> size.width * .66f
                    }
                    val h = when (shape) {
                        "Rond" -> size.height * .58f
                        "Anguleux" -> size.height * .72f
                        else -> size.height * .66f
                    }
                    val left = (size.width - w) / 2
                    val top = (size.height - h) / 2
                    drawRect(outline, Offset(left - 3f, top - 3f), Size(w + 6f, h + 6f))
                    drawRect(skin, Offset(left, top), Size(w, h))
                    drawRect(Color(0xFF202833), Offset(size.width*.34f, size.height*.47f), Size(4f,4f))
                    drawRect(Color(0xFF202833), Offset(size.width*.62f, size.height*.47f), Size(4f,4f))
                    drawRect(Color(0xFF6A3433), Offset(size.width*.42f, size.height*.66f), Size(size.width*.16f,3f))
                }
                Spacer(Modifier.height(4.dp))
                Text(shape.uppercase(), color = if (active) UltimateIvory else UltimateMuted, fontSize = 8.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PixelBodyShapeStrip(selected: String, civilianStyle: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(13.dp))
    Text("SILHOUETTE", color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        UltimateCatalog.bodyBuilds.forEach { build ->
            val active = build == selected
            Column(
                Modifier.width(90.dp)
                    .clip(CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .background(if (active) Color(0xFF142334) else Color(0xFF0D141C))
                    .border(1.dp, if (active) UltimateBlue else Color(0xFF2C3948), CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .clickable { onSelect(build) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.width(48.dp).height(68.dp)) {
                    val outline = Color(0xFF11161C)
                    val shirt = when {
                        civilianStyle.contains("Sport", true) -> Color(0xFF2E6CA4)
                        civilianStyle.contains("Créat", true) -> Color(0xFF7A3F7E)
                        civilianStyle.contains("Vintage", true) -> Color(0xFF795B3E)
                        else -> Color(0xFF2A415D)
                    }
                    val bodyW = when (build) {
                        "Fin" -> size.width*.42f
                        "Massif" -> size.width*.82f
                        "Robuste" -> size.width*.72f
                        else -> size.width*.60f
                    }
                    val x=(size.width-bodyW)/2
                    drawRect(outline, Offset(x-3f,size.height*.18f), Size(bodyW+6f,size.height*.56f))
                    drawRect(shirt, Offset(x,size.height*.22f), Size(bodyW,size.height*.48f))
                    drawRect(outline, Offset(size.width*.33f,size.height*.72f), Size(size.width*.12f,size.height*.24f))
                    drawRect(outline, Offset(size.width*.55f,size.height*.72f), Size(size.width*.12f,size.height*.24f))
                }
                Spacer(Modifier.height(4.dp))
                Text(build.uppercase(), color = if (active) UltimateIvory else UltimateMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}


@Composable
private fun PixelHairStrip(selected: String, hairColor: String, skinTone: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(13.dp))
    Text("COIFFURE", color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        UltimateCatalog.hairs.forEach { hair ->
            val active = hair == selected
            Column(
                Modifier.width(90.dp)
                    .clip(CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .background(if (active) Color(0xFF142334) else Color(0xFF0D141C))
                    .border(1.dp, if (active) UltimateBlue else Color(0xFF2C3948), CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .clickable { onSelect(hair) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.size(56.dp)) {
                    val skin = pixelSkinPreview(skinTone)
                    val hc = pixelHairPreview(hairColor)
                    val outline = Color(0xFF11161C)
                    val x = size.width*.22f
                    val y = size.height*.20f
                    val fw = size.width*.56f
                    val fh = size.height*.62f
                    drawRect(outline, Offset(x-3f,y-3f), Size(fw+6f,fh+6f))
                    drawRect(skin, Offset(x,y), Size(fw,fh))
                    when(hair) {
                        "Rasé" -> {
                            drawRect(hc, Offset(x,y-3f), Size(fw,5f))
                        }
                        "Long" -> {
                            drawRect(hc, Offset(x-5f,y-8f), Size(fw+10f,size.height*.22f))
                            drawRect(hc, Offset(x-5f,y+size.height*.10f), Size(7f,size.height*.50f))
                            drawRect(hc, Offset(x+fw-2f,y+size.height*.10f), Size(7f,size.height*.50f))
                        }
                        "Tresses" -> {
                            drawRect(hc, Offset(x,y-6f), Size(fw,9f))
                            repeat(4){i ->
                                drawRect(hc, Offset(x+4f+i*(fw-8f)/4f,y+4f), Size(3f,size.height*.46f))
                            }
                        }
                        "Boucles" -> {
                            repeat(6){i ->
                                val cx=x+(i%3)*fw*.32f
                                val cy=y-8f+(i/3)*size.height*.12f
                                drawRect(hc, Offset(cx,cy), Size(fw*.38f,size.height*.14f))
                            }
                        }
                        "Undercut" -> {
                            drawRect(hc, Offset(x+fw*.18f,y-10f), Size(fw*.82f,size.height*.15f))
                            drawRect(hc.copy(alpha=.6f), Offset(x,y), Size(fw*.16f,size.height*.18f))
                        }
                        else -> {
                            drawRect(hc, Offset(x,y-7f), Size(fw,size.height*.16f))
                            drawRect(hc, Offset(x,y), Size(fw*.18f,size.height*.12f))
                        }
                    }
                    drawRect(Color(0xFF26303A), Offset(x+fw*.25f,y+fh*.38f), Size(4f,4f))
                    drawRect(Color(0xFF26303A), Offset(x+fw*.68f,y+fh*.38f), Size(4f,4f))
                }
                Spacer(Modifier.height(4.dp))
                Text(hair.uppercase(), color = if (active) UltimateIvory else UltimateMuted, fontSize = 8.sp, fontWeight = FontWeight.Black, maxLines = 1)
            }
        }
    }
}

@Composable
private fun PixelStyleStrip(selected: String, onSelect: (String) -> Unit) {
    Spacer(Modifier.height(13.dp))
    Text("STYLE CIVIL", color = UltimateGold, fontSize = 9.sp, fontWeight = FontWeight.Black, letterSpacing = 1.1.sp)
    Spacer(Modifier.height(7.dp))
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        UltimateCatalog.civilianStyles.forEach { style ->
            val active = style == selected
            Column(
                Modifier.width(98.dp)
                    .clip(CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .background(if (active) Color(0xFF142334) else Color(0xFF0D141C))
                    .border(1.dp, if (active) UltimateBlue else Color(0xFF2C3948), CutCornerShape(topEnd = 12.dp, bottomStart = 12.dp))
                    .clickable { onSelect(style) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Canvas(Modifier.width(58.dp).height(64.dp)) {
                    val main = when {
                        style.contains("Sport", true) -> Color(0xFF2E6CA4)
                        style.contains("Class", true) -> Color(0xFF27303A)
                        style.contains("Créat", true) -> Color(0xFF7A3F7E)
                        style.contains("Profession", true) -> Color(0xFF3B4D68)
                        style.contains("Vintage", true) -> Color(0xFF795B3E)
                        else -> Color(0xFF2A415D)
                    }
                    val accent = when {
                        style.contains("Sport", true) -> Color(0xFFB9D9FF)
                        style.contains("Créat", true) -> Color(0xFFE3B7F0)
                        style.contains("Vintage", true) -> Color(0xFFE1C49F)
                        else -> Color(0xFFB9C7D8)
                    }
                    val outline=Color(0xFF10151B)
                    drawRect(outline, Offset(size.width*.18f,size.height*.16f), Size(size.width*.64f,size.height*.70f))
                    drawRect(main, Offset(size.width*.22f,size.height*.20f), Size(size.width*.56f,size.height*.62f))
                    drawRect(accent, Offset(size.width*.22f,size.height*.56f), Size(size.width*.56f,size.height*.08f))
                    if(style.contains("Class", true) || style.contains("Profession", true)) {
                        drawRect(accent, Offset(size.width*.47f,size.height*.20f), Size(size.width*.06f,size.height*.36f))
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(style.uppercase(), color = if (active) UltimateIvory else UltimateMuted, fontSize = 7.sp, fontWeight = FontWeight.Black, maxLines = 2, lineHeight = 9.sp)
            }
        }
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
