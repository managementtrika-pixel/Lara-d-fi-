package com.metahumanlegacy.game

import org.json.JSONArray

/** Preserve all creator options during rotation and process recreation. */
internal object FinalDraftCodec {
    fun encode(d: UltimateCreationDraft): String {
        val b = d.blueprint
        return JSONArray(listOf(b.firstName, b.lastName, b.pronouns, b.city, b.district,
            b.socialBackground, b.motivation, b.civilianPath, b.temperament,
            d.bodyBuild, d.stature, d.skinTone, d.faceShape, d.hair, d.hairColor,
            d.facialHair, d.eyes, d.civilianStyle, d.accessory, d.cityArchetype,
            d.climate, d.architecture, d.cityMood, d.libraryFaceIndex)).toString()
    }
    fun decode(raw: String): UltimateCreationDraft? = runCatching {
        val a = JSONArray(raw)
        require(a.length() == 24)
        fun s(i: Int) = a.getString(i)
        UltimateCreationDraft(CharacterBlueprint(s(0), s(1), s(2), s(3), s(4), s(5), s(6), s(7), s(8)),
            s(9), s(10), s(11), s(12), s(13), s(14), s(15), s(16), s(17), s(18),
            s(19), s(20), s(21), s(22), a.getInt(23))
    }.getOrNull()
}
