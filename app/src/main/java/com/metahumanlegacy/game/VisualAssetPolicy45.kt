package com.metahumanlegacy.game

/**
 * 4.5 visual-source gate.
 *
 * External art must be explicitly reviewed before it enters the shipped game. We prefer CC0/public
 * domain so the Android build never depends on unclear "royalty free" wording. This registry is
 * intentionally data-only: rendering remains deterministic and offline even when optional packs are
 * not bundled.
 */
internal enum class VisualAssetLicense { CC0, PUBLIC_DOMAIN, CC_BY, REJECT }

internal data class VisualAssetSource(
    val id: String,
    val label: String,
    val home: String,
    val license: VisualAssetLicense,
    val attributionRequired: Boolean,
    val approvedForCommercialBuild: Boolean,
    val use: String
)

internal object VisualAssetPolicy45 {
    val reviewedSources = listOf(
        VisualAssetSource(
            id = "kenney",
            label = "Kenney game assets",
            home = "https://kenney.nl/assets",
            license = VisualAssetLicense.CC0,
            attributionRequired = false,
            approvedForCommercialBuild = true,
            use = "UI primitives, environment props, tiles and placeholder sprites"
        ),
        VisualAssetSource(
            id = "opengameart-cc0",
            label = "OpenGameArt CC0 selections",
            home = "https://opengameart.org/",
            license = VisualAssetLicense.CC0,
            attributionRequired = false,
            approvedForCommercialBuild = true,
            use = "Curated textures, particles, props and environment details; per-item license must still be verified"
        ),
        VisualAssetSource(
            id = "itch-cc0",
            label = "itch.io CC0 pixel-art selections",
            home = "https://itch.io/game-assets/free/tag-cc0/tag-pixel-art",
            license = VisualAssetLicense.CC0,
            attributionRequired = false,
            approvedForCommercialBuild = true,
            use = "Curated pixel props, backgrounds and tiles; only packs explicitly marked CC0"
        )
    )

    fun canShip(source: VisualAssetSource): Boolean =
        source.approvedForCommercialBuild && source.license in setOf(VisualAssetLicense.CC0, VisualAssetLicense.PUBLIC_DOMAIN)

    fun requiresCredits(source: VisualAssetSource): Boolean = source.attributionRequired || source.license == VisualAssetLicense.CC_BY
}

/**
 * Visual details selected in the creator that the renderer must not silently ignore.
 * This closes a 4.4 audit gap: facial hair and accessories were part of the identity fingerprint but
 * were not represented by a dedicated render profile.
 */
internal data class PixelDetailProfile(
    val facialHair: String,
    val accessory: String,
    val hasFacialHair: Boolean,
    val hasAccessory: Boolean,
    val facialHairWeight: Int,
    val accessorySide: Int
)

internal fun pixelDetailProfile(state: UltimateState): PixelDetailProfile {
    val facial = state.facialHair.trim()
    val accessory = state.accessory.trim()
    val noFacial = facial.isBlank() || facial.equals("Aucun", true) || facial.equals("Aucune", true)
    val noAccessory = accessory.isBlank() || accessory.equals("Aucun", true) || accessory.equals("Aucune", true)
    val weight = when {
        noFacial -> 0
        facial.contains("barbe", true) -> 3
        facial.contains("bouc", true) -> 2
        facial.contains("moust", true) -> 1
        else -> 2
    }
    val side = if ((state.libraryFaceIndex.takeIf { it >= 0 } ?: state.hashCode()) and 1 == 0) -1 else 1
    return PixelDetailProfile(facial, accessory, !noFacial, !noAccessory, weight, side)
}
