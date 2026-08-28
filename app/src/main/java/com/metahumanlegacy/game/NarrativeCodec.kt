package com.metahumanlegacy.game

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipInputStream

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
    private const val PART_COUNT = 7
    private var bundleLoader: (() -> ByteArray)? = null
    private var entriesCache: Map<String, ByteArray>? = null

    fun installAssetParts(loader: (String) -> ByteArray) {
        installBundle {
            val out = ByteArrayOutputStream(120_000)
            for (index in 1..PART_COUNT) {
                val path = "narrative_bundle/rt16_${index.toString().padStart(2, '0')}.bin"
                out.write(loader(path))
            }
            out.toByteArray()
        }
    }

    fun installBundle(loader: () -> ByteArray) {
        bundleLoader = loader
        entriesCache = null
    }

    fun catalog(): List<Authored> = parseCatalog(textEntry("events.mhl"))
    fun effects(): Map<String, ExtraEffect> = parseEffects(textEntry("effects.tsv"))

    fun manifest(): Map<String, String> = textEntry("manifest.tsv")
        .lineSequence().filter { it.isNotBlank() }.associate { line ->
            val p = line.split('\t', limit = 2)
            p.first() to p.getOrElse(1) { "" }
        }

    private fun textEntry(name: String): String = entries()[name]?.toString(Charsets.UTF_8)
        ?: error("Narrative bundle missing required entry: $name")

    private fun entries(): Map<String, ByteArray> {
        entriesCache?.let { return it }
        val bytes = bundleLoader?.invoke() ?: error("Narrative bundle is not installed")
        val loaded = linkedMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (!entry.isDirectory) {
                    val out = ByteArrayOutputStream()
                    zip.copyTo(out)
                    loaded[entry.name] = out.toByteArray()
                }
                zip.closeEntry()
            }
        }
        require("events.mhl" in loaded && "effects.tsv" in loaded && "manifest.tsv" in loaded) {
            "Narrative bundle is incomplete: ${loaded.keys.sorted()}"
        }
        entriesCache = loaded
        return loaded
    }

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
}
