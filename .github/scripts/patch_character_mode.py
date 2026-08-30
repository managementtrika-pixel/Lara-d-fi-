from pathlib import Path
import re

# Persist an explicit illustrated-face choice without breaking old U1 saves.
state = Path('app/src/main/java/com/metahumanlegacy/game/UltimateState.kt')
s = state.read_text()
s = s.replace(
    '    val cityMood: String = "Contrastes sociaux"\n)',
    '    val cityMood: String = "Contrastes sociaux",\n    val libraryFaceIndex: Int = -1\n)',
    1,
)
s = s.replace(
    '    val snapshots: List<String> = emptyList(),\n    val lastProcessedTurn: Int = -1\n)',
    '    val snapshots: List<String> = emptyList(),\n    val lastProcessedTurn: Int = -1,\n    val libraryFaceIndex: Int = -1\n)',
    1,
)
s = s.replace(
    '            architecture = draft.architecture, cityMood = draft.cityMood,\n            journalist = relations.first',
    '            architecture = draft.architecture, cityMood = draft.cityMood,\n            libraryFaceIndex = draft.libraryFaceIndex,\n            journalist = relations.first',
    1,
)
old_return = 'return "U1|$scalar|${e(relations(state.relations))}|${e(cases(state.cases))}|${e(districts(state.districts))}|${e(list(state.techniques))}|${e(list(state.injuries))}|${e(list(state.iconicItems))}|${e(list(state.rareMarks))}|${e(list(state.memories))}|${e(list(state.snapshots))}"'
new_return = 'return "U1|$scalar|${e(relations(state.relations))}|${e(cases(state.cases))}|${e(districts(state.districts))}|${e(list(state.techniques))}|${e(list(state.injuries))}|${e(list(state.iconicItems))}|${e(list(state.rareMarks))}|${e(list(state.memories))}|${e(list(state.snapshots))}|${e(state.libraryFaceIndex)}"'
if old_return not in s:
    raise SystemExit('U1 encode return not found')
s = s.replace(old_return, new_return, 1)
old_tail = '        val rareMarks = decodeList(s()); val memories = decodeList(s()); val snapshots = decodeList(s())\n\n        UltimateState('
new_tail = '        val rareMarks = decodeList(s()); val memories = decodeList(s()); val snapshots = decodeList(s())\n        val libraryFaceIndex = s().toIntOrNull() ?: -1\n\n        UltimateState('
if old_tail not in s:
    raise SystemExit('U1 decode tail not found')
s = s.replace(old_tail, new_tail, 1)
s = s.replace(
    '            memories, snapshots, lastProcessedTurn\n        )',
    '            memories, snapshots, lastProcessedTurn, libraryFaceIndex = libraryFaceIndex\n        )',
    1,
)
state.write_text(s)

# One exact Library cell is the illustrated identity.
assets = Path('app/src/main/java/com/metahumanlegacy/game/LibraryCustomizationAssets.kt')
a = assets.read_text()
old_apply = '''    // One atlas cell is one facial base. Hair, beard, eyes and clothes stay independent.
    fun apply(draft: UltimateCreationDraft): UltimateCreationDraft = draft.copy(
        skinTone = skinTone,
        faceShape = faceShape
    )

    fun matches(draft: UltimateCreationDraft): Boolean =
        draft.skinTone == skinTone && draft.faceShape == faceShape'''
new_apply = '''    // One atlas cell is one atomic illustrated head. We never stack incompatible generated
    // face fragments over it. Editing a facial detail later deliberately switches to free mode.
    fun apply(draft: UltimateCreationDraft): UltimateCreationDraft = draft.copy(
        skinTone = skinTone,
        faceShape = faceShape,
        libraryFaceIndex = atlasIndex
    )

    fun matches(draft: UltimateCreationDraft): Boolean = draft.libraryFaceIndex == atlasIndex'''
if old_apply not in a:
    raise SystemExit('face apply block not found')
a = a.replace(old_apply, new_apply, 1)
a = a.replace(
    '        "Looks illustrés",\n        "Chaque vignette applique un preset complet aux paramètres stables. Aucun fragment généré n\'est superposé au hasard."',
    '        "Base du visage",\n        "Une vignette = une seule personne. Modifier teint, forme, coiffure, barbe ou yeux repasse automatiquement en mode libre pour éviter tout mauvais assemblage."',
    1,
)
assets.write_text(a)

# Runtime reads the explicit cell selection.
visuals = Path('app/src/main/java/com/metahumanlegacy/game/UltimateVisuals.kt')
v = visuals.read_text()
pattern = r'''private fun libraryFaceIndex\(state: UltimateState\): Int\? =\n    LibraryCustomizationCatalog\.facePresets\.firstOrNull \{ preset ->\n        preset\.skinTone == state\.skinTone && preset\.faceShape == state\.faceShape\n    \}\?\.atlasIndex'''
replacement = '''private fun libraryFaceIndex(state: UltimateState): Int? =
    state.libraryFaceIndex.takeIf {
        it in 0 until LibraryCustomizationCatalog.FACE_COLUMNS * LibraryCustomizationCatalog.FACE_ROWS
    }'''
v, count = re.subn(pattern, replacement, v, count=1)
if count != 1:
    raise SystemExit('runtime face selection block not found')
visuals.write_text(v)

# Facial edits switch to the existing safe procedural renderer instead of stacking generated parts.
screens = Path('app/src/main/java/com/metahumanlegacy/game/UltimateScreens.kt')
u = screens.read_text()
for old, new in {
    'onDraft(draft.copy(skinTone = it))': 'onDraft(draft.copy(skinTone = it, libraryFaceIndex = -1))',
    'onDraft(draft.copy(faceShape = it))': 'onDraft(draft.copy(faceShape = it, libraryFaceIndex = -1))',
    'onDraft(draft.copy(hair = it))': 'onDraft(draft.copy(hair = it, libraryFaceIndex = -1))',
    'onDraft(draft.copy(hairColor = it))': 'onDraft(draft.copy(hairColor = it, libraryFaceIndex = -1))',
    'onDraft(draft.copy(facialHair = it))': 'onDraft(draft.copy(facialHair = it, libraryFaceIndex = -1))',
    'onDraft(draft.copy(eyes = it))': 'onDraft(draft.copy(eyes = it, libraryFaceIndex = -1))',
}.items():
    if old not in u:
        raise SystemExit(f'creation edit hook missing: {old}')
    u = u.replace(old, new, 1)
screens.write_text(u)

# Regression test: one chosen thumbnail maps to one exact atlas cell and does not rewrite unrelated fields.
tests = Path('app/src/test/java/com/metahumanlegacy/game/LibraryCustomizationAssetTest.kt')
t = tests.read_text()
insert = '''

    @Test
    fun illustratedFaceSelectionUsesOneExactAtlasCell() {
        val blueprint = CharacterBlueprint(
            firstName = "Test", lastName = "Avatar", pronouns = "iel",
            city = GameEngine.cities.first(), district = GameEngine.districts.first(),
            socialBackground = GameEngine.socialBackgrounds.first(), motivation = GameEngine.motivations.first(),
            civilianPath = GameEngine.civilianPaths.first(), temperament = GameEngine.temperaments.first()
        )
        val draft = UltimateCreationDraft(blueprint = blueprint, hair = "Boucles", hairColor = "Roux")
        val preset = LibraryCustomizationCatalog.facePresets[7]
        val applied = preset.apply(draft)
        assertTrue(applied.libraryFaceIndex == preset.atlasIndex)
        assertTrue(applied.hair == "Boucles")
        assertTrue(applied.hairColor == "Roux")
        assertTrue(preset.matches(applied))
    }
'''
if 'illustratedFaceSelectionUsesOneExactAtlasCell' not in t:
    pos = t.rfind('\n}')
    if pos < 0:
        raise SystemExit('test class closing brace not found')
    t = t[:pos] + insert + t[pos:]
tests.write_text(t)
