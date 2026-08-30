from pathlib import Path
import re

assets = Path('app/src/main/java/com/metahumanlegacy/game/LibraryCustomizationAssets.kt')
a = assets.read_text()

old_apply = '''    // One atlas cell is one atomic illustrated head. We never stack incompatible generated
    // face fragments over it. Editing a facial detail later deliberately switches to free mode.
    fun apply(draft: UltimateCreationDraft): UltimateCreationDraft = draft.copy(
        skinTone = skinTone,
        faceShape = faceShape,
        libraryFaceIndex = atlasIndex
    )

    fun matches(draft: UltimateCreationDraft): Boolean = draft.libraryFaceIndex == atlasIndex'''
new_apply = '''    // An illustrated portrait is an atomic identity. It never rewrites the free-mode sliders and
    // no generated eyes/hair/beard are stacked on top of it.
    fun apply(draft: UltimateCreationDraft): UltimateCreationDraft = draft.copy(
        libraryFaceIndex = atlasIndex
    )

    fun matches(draft: UltimateCreationDraft): Boolean = draft.libraryFaceIndex == atlasIndex'''
if old_apply not in a:
    raise SystemExit('LibraryFacePreset apply block not found')
a = a.replace(old_apply, new_apply, 1)

tones = ['Très clair','Clair','Moyen','Mat','Foncé','Très foncé']
shapes = ['Ovale','Carré','Fin','Rond','Anguleux']
lines = []
for i in range(48):
    tone = tones[(i // 8) % len(tones)]
    shape = shapes[i % len(shapes)]
    lines.append(f'        LibraryFacePreset("Visage {i+1:02d}", {i}, "{tone}", "{shape}", "Rasé", "Noir", "Aucune", "Bruns", "Street sobre")')
face_list = '    val facePresets = listOf(\n' + ',\n'.join(lines) + '\n    )\n\n'
a, n = re.subn(r'    val facePresets = listOf\(.*?\n    \)\n\n    // The source atlas', face_list + '    // The source atlas', a, count=1, flags=re.S)
if n != 1:
    raise SystemExit('facePresets list not found')

old_header = '''    LibraryPresetHeader(
        "Base du visage",
        "Une vignette = une seule personne. Modifier teint, forme, coiffure, barbe ou yeux repasse automatiquement en mode libre pour éviter tout mauvais assemblage."
    )'''
new_header = '''    LibraryPresetHeader(
        "Portrait illustré",
        "48 identités individuelles. Une vignette = une personne complète : aucun morceau de planche, aucun fragment incompatible superposé."
    )'''
if old_header not in a:
    raise SystemExit('face preset header not found')
a = a.replace(old_header, new_header, 1)
assets.write_text(a)

visuals = Path('app/src/main/java/com/metahumanlegacy/game/UltimateVisuals.kt')
v = visuals.read_text()
start = v.find('        if (libraryAtlas != null && libraryIndex != null) {')
end = v.find('\n        val skin = skinColor(state.skinTone)', start)
if start < 0 or end < 0:
    raise SystemExit('illustrated portrait branch not found')
new_branch = '''        if (libraryAtlas != null && libraryIndex != null) {
            // Illustrated mode: one transparent head from the curated 8x6 Library atlas is placed
            // on a coherent game-rendered torso. No square card, no atlas background, no facial
            // fragments or procedural mask are drawn over the identity.
            val clothes = if (heroMode) paletteColors(state.costumePalette, c.powerFamily)
                else outfitColor(state.civilianStyle) to Color(0xFF10151C)
            val bodyWidth = when (state.bodyBuild) {
                "Fin" -> .35f
                "Massif" -> .54f
                "Robuste" -> .49f
                else -> .43f
            }
            val shoulderY = h * .66f
            val torso = Path().apply {
                moveTo(w * (.5f - bodyWidth / 2), h)
                lineTo(w * (.5f - bodyWidth / 2.05f), shoulderY)
                quadraticBezierTo(w * .5f, h * .585f, w * (.5f + bodyWidth / 2.05f), shoulderY)
                lineTo(w * (.5f + bodyWidth / 2), h)
                close()
            }
            drawPath(torso, clothes.first)
            drawPath(torso, clothes.second.copy(alpha = .90f), style = Stroke(w * .014f))
            if (heroMode) {
                drawLine(clothes.second.copy(alpha = .80f), Offset(w * .36f, h * .73f), Offset(w * .64f, h * .73f), w * .012f)
                drawLine(profile.accent.copy(alpha = .60f), Offset(w * .40f, h * .78f), Offset(w * .60f, h * .78f), w * .006f)
            }

            val sourceW = libraryAtlas.width / LibraryCustomizationCatalog.FACE_COLUMNS
            val sourceH = libraryAtlas.height / LibraryCustomizationCatalog.FACE_ROWS
            val col = libraryIndex % LibraryCustomizationCatalog.FACE_COLUMNS
            val row = libraryIndex / LibraryCustomizationCatalog.FACE_COLUMNS
            val faceSide = minOf(w * .82f, h * .61f)
            val faceLeft = (w - faceSide) / 2f
            val faceTop = h * .075f
            drawImage(
                image = libraryAtlas,
                srcOffset = IntOffset(col * sourceW, row * sourceH),
                srcSize = IntSize(sourceW, sourceH),
                dstOffset = IntOffset(faceLeft.toInt(), faceTop.toInt()),
                dstSize = IntSize(faceSide.toInt().coerceAtLeast(1), faceSide.toInt().coerceAtLeast(1))
            )

            drawRoundRect(
                color = (if (heroMode) profile.accent else UltimateGold).copy(alpha = .55f),
                topLeft = Offset(w * .035f, h * .025f),
                size = Size(w * .93f, h * .95f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * .04f),
                style = Stroke(w * .008f)
            )
            if (heroMode && state.emblem != "Aucun") {
                drawEmblem(state.emblem, clothes.second, Offset(w * .5f, h * .845f), w * .09f)
            }
            if (showAura && heroMode) {
                drawCircle(profile.accent.copy(alpha = .22f), radius = w * .32f, center = Offset(w * .5f, h * .40f), style = Stroke(w * .010f))
            }
            drawRect(UltimateGold.copy(alpha = .72f), topLeft = Offset(w * .08f, h - w * .018f), size = Size(w * .84f, w * .018f))
            return@Canvas
        }
'''
v = v[:start] + new_branch + v[end:]
visuals.write_text(v)

screens = Path('app/src/main/java/com/metahumanlegacy/game/UltimateScreens.kt')
u = screens.read_text()
old_step1 = '''                        LibraryFacePresetStrip(draft, onDraft)
                        OptionStrip("SILHOUETTE", UltimateCatalog.bodyBuilds, draft.bodyBuild) { onDraft(draft.copy(bodyBuild = it)) }
                        OptionStrip("TAILLE", UltimateCatalog.statures, draft.stature) { onDraft(draft.copy(stature = it)) }
                        OptionStrip("TEINT", UltimateCatalog.skinTones, draft.skinTone) { onDraft(draft.copy(skinTone = it, libraryFaceIndex = -1)) }
                        OptionStrip("VISAGE", UltimateCatalog.faceShapes, draft.faceShape) { onDraft(draft.copy(faceShape = it, libraryFaceIndex = -1)) }
                        OptionStrip("COIFFURE", UltimateCatalog.hairs, draft.hair) { onDraft(draft.copy(hair = it, libraryFaceIndex = -1)) }'''
new_step1 = '''                        LibraryFacePresetStrip(draft, onDraft)
                        Spacer(Modifier.height(8.dp))
                        if (draft.libraryFaceIndex >= 0) {
                            UltimatePanel(accent = UltimateGold) {
                                Text("MODE ILLUSTRÉ", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                Text("Ce visage est une illustration complète. Le jeu ne colle jamais des yeux, cheveux ou barbes incompatibles par-dessus.", color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
                                Spacer(Modifier.height(8.dp))
                                MhlSecondaryButton("PASSER EN MODE LIBRE", { onDraft(draft.copy(libraryFaceIndex = -1)) }, Modifier.fillMaxWidth())
                            }
                        } else {
                            UltimatePanel(accent = UltimateBlue) {
                                Text("MODE LIBRE", color = UltimateBlue, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                Text("Construis le visage avec les paramètres compatibles du renderer. Choisis un portrait ci-dessus pour revenir au mode illustré.", color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                        OptionStrip("SILHOUETTE", UltimateCatalog.bodyBuilds, draft.bodyBuild) { onDraft(draft.copy(bodyBuild = it)) }
                        OptionStrip("TAILLE", UltimateCatalog.statures, draft.stature) { onDraft(draft.copy(stature = it)) }
                        if (draft.libraryFaceIndex < 0) {
                            OptionStrip("TEINT", UltimateCatalog.skinTones, draft.skinTone) { onDraft(draft.copy(skinTone = it)) }
                            OptionStrip("VISAGE", UltimateCatalog.faceShapes, draft.faceShape) { onDraft(draft.copy(faceShape = it)) }
                            OptionStrip("COIFFURE", UltimateCatalog.hairs, draft.hair) { onDraft(draft.copy(hair = it)) }
                        }'''
if old_step1 not in u:
    raise SystemExit('creation appearance block not found')
u = u.replace(old_step1, new_step1, 1)

old_step2 = '''                        OptionStrip("COULEUR DE CHEVEUX", UltimateCatalog.hairColors, draft.hairColor) { onDraft(draft.copy(hairColor = it, libraryFaceIndex = -1)) }
                        OptionStrip("BARBE", UltimateCatalog.facialHairs, draft.facialHair) { onDraft(draft.copy(facialHair = it, libraryFaceIndex = -1)) }
                        OptionStrip("YEUX", UltimateCatalog.eyes, draft.eyes) { onDraft(draft.copy(eyes = it, libraryFaceIndex = -1)) }
                        OptionStrip("VÊTEMENTS", UltimateCatalog.civilianStyles, draft.civilianStyle) { onDraft(draft.copy(civilianStyle = it)) }'''
new_step2 = '''                        if (draft.libraryFaceIndex < 0) {
                            OptionStrip("COULEUR DE CHEVEUX", UltimateCatalog.hairColors, draft.hairColor) { onDraft(draft.copy(hairColor = it)) }
                            OptionStrip("BARBE", UltimateCatalog.facialHairs, draft.facialHair) { onDraft(draft.copy(facialHair = it)) }
                            OptionStrip("YEUX", UltimateCatalog.eyes, draft.eyes) { onDraft(draft.copy(eyes = it)) }
                        } else {
                            UltimatePanel(accent = UltimateGold) {
                                Text("IDENTITÉ VISUELLE CONSERVÉE", color = UltimateGold, fontWeight = FontWeight.Black, fontSize = 10.sp)
                                Text("Cheveux, yeux et pilosité appartiennent au portrait illustré choisi. Les vêtements et accessoires restent personnalisables indépendamment.", color = UltimateMuted, fontSize = 11.sp, lineHeight = 16.sp)
                            }
                        }
                        OptionStrip("VÊTEMENTS", UltimateCatalog.civilianStyles, draft.civilianStyle) { onDraft(draft.copy(civilianStyle = it)) }'''
if old_step2 not in u:
    raise SystemExit('civil style face controls block not found')
u = u.replace(old_step2, new_step2, 1)
screens.write_text(u)

tests = Path('app/src/test/java/com/metahumanlegacy/game/LibraryCustomizationAssetTest.kt')
t = tests.read_text()
t = t.replace('assertTrue(LibraryCustomizationCatalog.facePresets.size == 30)', 'assertTrue(LibraryCustomizationCatalog.facePresets.size == 48)')
t = t.replace('assertTrue(LibraryCustomizationCatalog.facePresets.map { it.skinTone to it.faceShape }.distinct().size == 30)', 'assertTrue(LibraryCustomizationCatalog.facePresets.map { it.atlasIndex }.distinct().size == 48)')
t = t.replace('''        assertTrue(applied.libraryFaceIndex == preset.atlasIndex)
        assertTrue(applied.hair == "Boucles")
        assertTrue(applied.hairColor == "Roux")
        assertTrue(preset.matches(applied))''', '''        assertTrue(applied.libraryFaceIndex == preset.atlasIndex)
        assertTrue(applied.hair == "Boucles")
        assertTrue(applied.hairColor == "Roux")
        assertTrue(applied.skinTone == draft.skinTone)
        assertTrue(applied.faceShape == draft.faceShape)
        assertTrue(preset.matches(applied))''')
if 'illustratedFaceCatalogCoversWholeSheet' not in t:
    t = t.replace('\n}\n', '''

    @Test
    fun illustratedFaceCatalogCoversWholeSheet() {
        assertTrue(LibraryCustomizationCatalog.facePresets.map { it.atlasIndex }.sorted() == (0 until 48).toList())
    }
}\n''')
tests.write_text(t)
