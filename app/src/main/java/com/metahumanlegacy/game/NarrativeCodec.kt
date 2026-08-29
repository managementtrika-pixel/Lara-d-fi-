package com.metahumanlegacy.game

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

internal data class NarrativeConstat(val title: String, val text: String)

internal object NarrativeCodec {
    private val bundleParts = (1..10).map { "nobody_bundle/nb_${it.toString().padStart(2, '0')}.b64" }
    private val constatParts = (1..9).map { "constats_bundle/ct_${it.toString().padStart(2, '0')}.b64" }
    private var bundleLoader: (() -> ByteArray)? = null
    private var constatLoader: (() -> ByteArray)? = null
    private var entriesCache: Map<String, ByteArray>? = null
    private var constatsCache: Map<String, NarrativeConstat>? = null

    fun installAssetParts(loader: (String) -> ByteArray) {
        installBundle {
            val out = ByteArrayOutputStream(80_000)
            for (path in bundleParts) {
                val encoded = loader(path).toString(Charsets.US_ASCII).trim()
                out.write(decodeBase64(encoded))
            }
            out.toByteArray()
        }
        installConstats {
            val encoded = buildString(65_000) {
                for (path in constatParts) {
                    append(loader(path).toString(Charsets.US_ASCII).trim())
                }
            }
            GZIPInputStream(ByteArrayInputStream(decodeBase64(encoded))).use { it.readBytes() }
        }
    }

    fun installBundle(loader: () -> ByteArray) {
        bundleLoader = loader
        entriesCache = null
    }

    private fun installConstats(loader: () -> ByteArray) {
        constatLoader = loader
        constatsCache = null
    }

    fun prologue(): List<FormativeChapter> = parsePrologue(textEntry("prologue.tsv"))
    fun awakening(): AwakeningScene = parseAwakening(textEntry("awakening.tsv"))
    fun foundation(): List<FoundationScene> = parseFoundation(textEntry("foundation.tsv"))
    fun beats(): List<MajorBeat> = parseBeats(textEntry("arcs.tsv"))
    fun endings(): Map<String, Map<String, String>> = parseEndings(textEntry("endings.tsv"))
    fun constat(choiceId: String): NarrativeConstat? = constats()[choiceId]
    fun constatCount(): Int = constats().size

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
        val required = listOf("prologue.tsv", "awakening.tsv", "foundation.tsv", "arcs.tsv", "endings.tsv", "manifest.tsv")
        require(required.all { it in loaded }) { "Narrative bundle is incomplete: ${loaded.keys.sorted()}" }
        verifyBundle(loaded)
        entriesCache = loaded
        return loaded
    }

    private fun constats(): Map<String, NarrativeConstat> {
        constatsCache?.let { return it }
        val text = constatLoader?.invoke()?.toString(Charsets.UTF_8)
            ?: error("Constat bundle is not installed")
        val loaded = linkedMapOf<String, NarrativeConstat>()
        for (line in text.lineSequence().filter { it.isNotBlank() }) {
            val p = line.split('\t', limit = 3)
            require(p.size == 3) { "Malformed constat row" }
            loaded[p[0]] = NarrativeConstat(restoreConstat(p[1]), restoreConstat(p[2]))
        }
        require(loaded.size == 1064) { "Expected 1064 constats, got ${loaded.size}" }
        constatsCache = loaded
        return loaded
    }

    private fun verifyBundle(entries: Map<String, ByteArray>) {
        val specs = entries.getValue("manifest.tsv").toString(Charsets.UTF_8).lineSequence()
            .filter { it.isNotBlank() && it.contains('\t') }
            .map { it.split('\t') }
            .filter { it.size >= 3 && it[0].endsWith(".tsv") }
            .associateBy { it[0] }
        for (name in listOf("prologue.tsv", "awakening.tsv", "foundation.tsv", "arcs.tsv", "endings.tsv")) {
            val spec = specs[name] ?: error("Narrative manifest missing $name")
            val data = entries.getValue(name)
            require(data.size == spec[1].toInt()) { "$name size mismatch" }
            require(sha256(data) == spec[2]) { "$name SHA-256 mismatch" }
        }
    }

    private fun parsePrologue(text: String): List<FormativeChapter> = text.lineSequence()
        .filter { it.isNotBlank() }.map { line ->
            val p = line.split('\t', limit = 5)
            val choices = p[4].split(";;").map { raw ->
                val c = raw.split('~', limit = 8)
                Choice(
                    label = restore(c[0]),
                    moral = c[1].toInt(),
                    relationDelta = c[2].toInt(),
                    risk = riskValue(c[3]),
                    approach = "FORMATIVE",
                    stakes = if (c[3] == "HIGH") 2 else 1,
                    sourceCategory = "VIE",
                    flag = c[7].replace(',', '+'),
                    affinityDelta = csv(c[4]),
                    expressionDelta = csv(c[5]),
                    costDelta = csv(c[6])
                )
            }
            FormativeChapter(p[0], p[1].toInt(), restore(p[2]), restore(p[3]), choices)
        }.toList()

    private fun parseAwakening(text: String): AwakeningScene {
        val p = text.trim().split('\t', limit = 6)
        val choices = p[5].split(";;").map { raw ->
            val c = raw.split('~', limit = 3)
            val route = when (c[1]) {
                "SECRET_CONTROL" -> "ORDER"
                "FIRST_HEROIC_USE" -> "CARE"
                "MASTERY" -> "TRUTH"
                "SELF_INTEREST" -> "ASCEND"
                else -> "ORDER"
            }
            Choice(
                label = restore(c[0]), approach = route, stakes = 2, sourceCategory = "ÉVEIL",
                flag = c.getOrElse(2) { "" }.replace(',', '+')
            )
        }
        return AwakeningScene(
            p[0], p[1].toInt(), restore(p[2]), restore(p[3]),
            csv(p[4]).toSet(), choices
        )
    }

    private fun parseFoundation(text: String): List<FoundationScene> = text.lineSequence()
        .filter { it.isNotBlank() }.map { line ->
            val p = line.split('\t', limit = 5)
            val choices = p[4].split(";;").map { raw ->
                val c = raw.split('~', limit = 3)
                Choice(
                    label = restore(c[0]), approach = c[1], stakes = 1,
                    sourceCategory = "FONDATION", flag = c[2],
                    moral = when (c[1]) { "CARE" -> 2; "ASCEND" -> -1; else -> 0 },
                    opinion = when (c[1]) { "CARE" -> 1; "ASCEND" -> -1; else -> 0 },
                    fear = if (c[1] == "ASCEND") 1 else 0,
                    impact = if (c[1] == "ASCEND") 2 else 1,
                    identityDelta = if (c[1] in setOf("TRUTH", "ASCEND")) 2 else 0
                )
            }
            FoundationScene(p[0], p[1].toInt(), restore(p[2]), restore(p[3]), choices)
        }.toList()

    private fun parseBeats(text: String): List<MajorBeat> = text.lineSequence()
        .filter { it.isNotBlank() }.map { line ->
            val p = line.split('\t', limit = 12)
            val choices = p[11].split(";;").map { raw ->
                val c = raw.split('~', limit = 14)
                Choice(
                    label = restore(c[0]), approach = c[1],
                    moral = c[2].toInt(), prestige = c[3].toInt(), opinion = c[4].toInt(),
                    fear = c[5].toInt(), power = c[6].toInt(), impact = c[7].toInt(),
                    relationDelta = c[8].toInt(), identityDelta = c[9].toInt(), healthDelta = c[10].toInt(),
                    risk = riskValue(c[11]), flag = c[12].replace(',', '+'),
                    deferredHook = c[13].toBooleanStrictOrNull() ?: false
                )
            }
            MajorBeat(
                id = p[0], arc = p[1], stage = p[2].toInt(),
                minAge = p[3].toInt(), maxAge = p[4].toInt(), minScope = scopeOf(p[5]),
                requiresFlags = csv(p[6]).toSet(), tags = csv(p[7]),
                title = restore(p[8]), text = restore(p[9]), callbacks = p[10].split("||").filter { it.isNotBlank() }.map(::restore),
                choices = choices
            )
        }.toList()

    private fun parseEndings(text: String): Map<String, Map<String, String>> = buildMap {
        for (line in text.lineSequence().filter { it.isNotBlank() }) {
            val p = line.split('\t', limit = 5)
            put(p[0], mapOf(
                "CARE" to restore(p[1]), "ORDER" to restore(p[2]),
                "TRUTH" to restore(p[3]), "ASCEND" to restore(p[4])
            ))
        }
    }

    private fun restore(value: String) = value.replace("\\n", "\n")

    private fun restoreConstat(value: String): String {
        val out = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            if (value[i] == '\\' && i + 1 < value.length) {
                when (value[i + 1]) {
                    'n' -> { out.append('\n'); i += 2 }
                    't' -> { out.append('\t'); i += 2 }
                    '\\' -> { out.append('\\'); i += 2 }
                    else -> { out.append(value[i]); i++ }
                }
            } else {
                out.append(value[i])
                i++
            }
        }
        return out.toString()
    }

    private fun csv(value: String) = value.split(',').map { it.trim() }.filter { it.isNotBlank() }

    private fun riskValue(raw: String): Int = when (raw.uppercase()) {
        "EXTREME" -> 9
        "HIGH" -> 7
        "MEDIUM" -> 4
        "LOW" -> 2
        else -> 1
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }

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
            val value = table[code]
            if (value < 0) continue
            buffer = (buffer shl 6) or value
            bits += 6
            if (bits >= 8) {
                bits -= 8
                out.write((buffer shr bits) and 0xFF)
            }
        }
        return out.toByteArray()
    }
}
