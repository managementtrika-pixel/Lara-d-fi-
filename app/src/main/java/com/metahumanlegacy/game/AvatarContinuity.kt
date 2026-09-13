package com.metahumanlegacy.game

/**
 * Immutable identity of the procedural avatar. Story progression may add costume/age presentation,
 * but these creator-selected features must not silently change between preview, save, load or gameplay.
 */
internal data class AvatarAppearanceFingerprint(
    val bodyBuild: String,
    val stature: String,
    val skinTone: String,
    val faceShape: String,
    val hair: String,
    val hairColor: String,
    val facialHair: String,
    val eyes: String,
    val civilianStyle: String,
    val accessory: String,
    val libraryFaceIndex: Int
)

internal fun UltimateCreationDraft.appearanceFingerprint(): AvatarAppearanceFingerprint =
    AvatarAppearanceFingerprint(
        bodyBuild = bodyBuild,
        stature = stature,
        skinTone = skinTone,
        faceShape = faceShape,
        hair = hair,
        hairColor = hairColor,
        facialHair = facialHair,
        eyes = eyes,
        civilianStyle = civilianStyle,
        accessory = accessory,
        libraryFaceIndex = libraryFaceIndex
    )

internal fun UltimateState.appearanceFingerprint(): AvatarAppearanceFingerprint =
    AvatarAppearanceFingerprint(
        bodyBuild = bodyBuild,
        stature = stature,
        skinTone = skinTone,
        faceShape = faceShape,
        hair = hair,
        hairColor = hairColor,
        facialHair = facialHair,
        eyes = eyes,
        civilianStyle = civilianStyle,
        accessory = accessory,
        libraryFaceIndex = libraryFaceIndex
    )
