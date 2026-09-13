package com.metahumanlegacy.game

/**
 * Stable micro-geometry for the procedural face. The same UltimateState must render the same
 * recognizable person everywhere; costume and age may layer on top but never reroll this DNA.
 */
internal data class PixelFaceIdentity(
    val eyeInset: Int,
    val eyeLevel: Int,
    val noseOffset: Int,
    val mouthInset: Int,
    val browOffset: Int,
    val cheekMark: Int
)

internal fun pixelFaceIdentity(state: UltimateState): PixelFaceIdentity {
    // Old library-face saves remain meaningful when present. New assetless characters derive their
    // signature from the exact creator-selected traits instead of a narrative/random seed.
    val source = if (state.libraryFaceIndex >= 0) {
        "library:${state.libraryFaceIndex}|${state.faceShape}"
    } else {
        listOf(state.faceShape, state.eyes, state.skinTone, state.hair, state.hairColor).joinToString("|")
    }
    val h = source.hashCode().toUInt().toLong()
    return PixelFaceIdentity(
        eyeInset = ((h ushr 1) % 2L).toInt(),
        eyeLevel = ((h ushr 3) % 3L).toInt() - 1,
        noseOffset = ((h ushr 6) % 3L).toInt() - 1,
        mouthInset = ((h ushr 9) % 2L).toInt(),
        browOffset = ((h ushr 11) % 3L).toInt() - 1,
        cheekMark = ((h ushr 14) % 3L).toInt()
    )
}

internal fun pixelFaceIdentityKey(state: UltimateState): String {
    val face = pixelFaceIdentity(state)
    return listOf(
        state.faceShape,
        state.skinTone,
        state.eyes,
        state.hair,
        state.hairColor,
        state.libraryFaceIndex,
        face.eyeInset,
        face.eyeLevel,
        face.noseOffset,
        face.mouthInset,
        face.browOffset,
        face.cheekMark
    ).joinToString("|")
}
