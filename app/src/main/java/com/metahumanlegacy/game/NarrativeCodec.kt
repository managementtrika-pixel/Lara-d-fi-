package com.metahumanlegacy.game

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream

internal data class Authored(
    val id: String, val family: String, val arc: String, val minAge: Int, val maxAge: Int,
    val rarity: String, val weight: Double, val cooldown: Int, val once: Boolean,
    val phase: String, val scopeMin: String, val originReq: String, val requiredFlag: String,
    val forbiddenFlag: String, val title: String, val text: String, val choices: List<AuthChoice>
)

internal data class AuthChoice(
    val label: String, val moral: Int, val prestige: Int, val opinion: Int, val fear: Int,
    val risk: Int, val tagToken: String, val flags: List<String>
)

internal data class ExtraEffect(
    val power: Int, val influence: Int, val relation: Int, val identity: Int,
    val health: Int, val riskLabel: String, val flags: List<String>
)

internal object NarrativeCodec {
    fun catalog(): List<Authored> = parseCatalog(gunzipBase64(
        listOf(
            NARRATIVE_PART_01, NARRATIVE_PART_02, NARRATIVE_PART_03, NARRATIVE_PART_04, NARRATIVE_PART_05,
            NARRATIVE_PART_06, NARRATIVE_PART_07, NARRATIVE_PART_08, NARRATIVE_PART_09, NARRATIVE_PART_10,
            NARRATIVE_PART_11, NARRATIVE_PART_12, NARRATIVE_PART_13, NARRATIVE_PART_14
        ).joinToString("")
    ))

    fun effects(): Map<String, ExtraEffect> = parseEffects(
        gunzipBase64(listOf(EFFECTS_PART_01, EFFECTS_PART_02).joinToString(""))
    )

    private fun parseCatalog(text: String): List<Authored> = buildList {
        for (line in text.lineSequence()) {
            if (line.isBlank() || line.startsWith("#")) continue
            val c = line.split('\t', limit = 17)
            if (c.size < 17) continue
            val choices = buildList {
                for (raw in c[16].split(";;")) {
                    if (raw.isBlank()) continue
                    val p = raw.split('~', limit = 7)
                    if (p.size < 7) continue
                    val tags = p[6].split('+').filter { it.isNotBlank() }
                    add(AuthChoice(
                        p[0], p[1].toIntOrNull() ?: 0, p[2].toIntOrNull() ?: 0,
                        p[3].toIntOrNull() ?: 0, p[4].toIntOrNull() ?: 0, p[5].toIntOrNull() ?: 0,
                        tags.firstOrNull().orEmpty(), tags.drop(1)
                    ))
                }
            }
            add(Authored(
                c[0], c[1], c[2], c[3].toIntOrNull() ?: 18, c[4].toIntOrNull() ?: 99,
                c[5], c[6].toDoubleOrNull() ?: 1.0, c[7].toIntOrNull() ?: 0,
                c[8].equals("true", true), c[9], c[10], c[11], c[12], c[13], c[14], c[15], choices
            ))
        }
    }

    private fun parseEffects(text: String): Map<String, ExtraEffect> = buildMap {
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val c = line.split('\t', limit = 8)
            if (c.size < 8) continue
            put(c[0], ExtraEffect(
                c[1].toIntOrNull() ?: 0, c[2].toIntOrNull() ?: 0, c[3].toIntOrNull() ?: 0,
                c[4].toIntOrNull() ?: 0, c[5].toIntOrNull() ?: 0, c[6], c[7].split(',').filter { it.isNotBlank() }
            ))
        }
    }

    private fun gunzipBase64(input: String): String {
        val bytes = decodeBase64(input)
        return GZIPInputStream(ByteArrayInputStream(bytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
    }

    private fun decodeBase64(input: String): ByteArray {
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val table = IntArray(256) { -1 }
        for (i in alphabet.indices) table[alphabet[i].code] = i
        val out = ByteArrayOutputStream(input.length * 3 / 4)
        var buffer = 0
        var bits = 0
        for (ch in input) {
            if (ch == '=') break
            val code = ch.code
            if (code >= table.size) continue
            val v = table[code]
            if (v < 0) continue
            buffer = (buffer shl 6) or v
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}
