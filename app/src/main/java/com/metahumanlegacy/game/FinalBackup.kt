package com.metahumanlegacy.game

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.InputStream

internal data class FinalBackupData(val session: FinalSession?, val hall: List<String>, val deepHall: String)

internal object FinalBackupStore {
    private const val MAX_BYTES = 2 * 1024 * 1024
    fun export(context: Context): String = JSONObject()
        .put("format", "METAHUMAN_LEGACY_BACKUP").put("version", 5)
        .put("session", FinalSessionPersistence.load(context)?.let { JSONObject(FinalSessionPersistence.encode(it)) } ?: JSONObject.NULL)
        .put("hall", JSONArray(loadHallV4(context)))
        .put("deepHall", JSONArray(context.getSharedPreferences("mhl_deep_legacy_v2", Context.MODE_PRIVATE)
            .getString("records", "[]") ?: "[]")).toString()

    fun read(input: InputStream): FinalBackupData {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var count = input.read(buffer)
        while (count != -1) {
            require(output.size() + count <= MAX_BYTES) { "Fichier trop volumineux" }
            output.write(buffer, 0, count)
            count = input.read(buffer)
        }
        return decode(output.toString("UTF-8"))
    }
    fun decode(raw: String): FinalBackupData {
        val root = JSONObject(raw)
        require(root.getString("format") == "METAHUMAN_LEGACY_BACKUP" && root.getInt("version") == 5)
        val session = if (root.isNull("session")) null else
            requireNotNull(FinalSessionPersistence.decode(root.getJSONObject("session").toString()))
        val arr = root.getJSONArray("hall")
        val deep = root.getJSONArray("deepHall")
        require(arr.length() <= 60 && deep.length() <= 60)
        val hall = (0 until arr.length()).map { arr.getString(it) }
        hall.forEach { require(it.length <= 32768) }
        for (i in 0 until deep.length()) deep.getJSONObject(i)
        return FinalBackupData(session?.let(FinalSessionPersistence::normalized), hall, deep.toString())
    }
    fun restore(context: Context, data: FinalBackupData) {
        data.session?.let { requireNotNull(FinalSessionPersistence.decode(FinalSessionPersistence.encode(it))) }
        val edit = context.getSharedPreferences("legacy", Context.MODE_PRIVATE).edit()
            .putString("hall", data.hall.joinToString(";;")).remove(FinalSessionPersistence.PREVIOUS)
        val session = data.session
        if (session == null) edit.remove("campaign").remove(FinalSessionPersistence.ACTIVE)
        else edit.putString("campaign", encodeCampaignV4(session.campaign))
            .putString(FinalSessionPersistence.ACTIVE, FinalSessionPersistence.encode(session))
        edit.apply()
        context.getSharedPreferences("mhl_deep_legacy_v2", Context.MODE_PRIVATE).edit()
            .putString("records", data.deepHall).apply()
    }
}

@Composable
internal fun FinalBackupPanel(context: Context, onImported: () -> Unit) {
    val scope = rememberCoroutineScope()
    var notice by remember { mutableStateOf<String?>(null) }
    var candidate by remember { mutableStateOf<FinalBackupData?>(null) }
    var busy by remember { mutableStateOf(false) }
    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val saved = withContext(Dispatchers.IO) {
                runCatching {
                    val raw = FinalBackupStore.export(context)
                    requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).use { it.write(raw.toByteArray(Charsets.UTF_8)) }
                }.isSuccess
            }
            notice = if (saved) "Sauvegarde exportée : ta vie et ton Hall sont conservés." else "L'export n'a pas abouti. Ta partie reste sur cet appareil."
            busy = false
        }
    }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) scope.launch {
            busy = true
            val result = withContext(Dispatchers.IO) {
                runCatching { requireNotNull(context.contentResolver.openInputStream(uri)).use(FinalBackupStore::read) }
            }
            candidate = result.getOrNull()
            if (result.isFailure) notice = "Ce fichier n'est pas une sauvegarde MetaHuman Legacy valide. Ta partie est conservée."
            busy = false
        }
    }
    candidate?.let { data ->
        AlertDialog(
            onDismissRequest = { candidate = null },
            title = { Text("Restaurer cette sauvegarde ?") },
            text = { Text((data.session?.campaign?.let { "${it.alias.ifBlank { it.name }}, ${it.age} ans. " } ?: "Aucune vie en cours. ") +
                "Cette sauvegarde remplacera la vie et le Hall actuellement sur cet appareil.") },
            confirmButton = { TextButton(onClick = {
                FinalBackupStore.restore(context, data)
                candidate = null
                onImported()
            }) { Text("RESTAURER") } },
            dismissButton = { TextButton(onClick = { candidate = null }) { Text("ANNULER") } }
        )
    }
    Spacer(Modifier.height(10.dp))
    UltimatePanel(accent = UltimateBlue) {
        Text("GARDER MES VIES", color = UltimateBlue, fontSize = 12.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(5.dp))
        Text("Exporte ta partie et ton Hall pour les conserver ou les retrouver sur un autre appareil.", color = UltimateMuted, fontSize = 14.sp, lineHeight = 21.sp)
        Spacer(Modifier.height(12.dp))
        MhlPrimaryButton("Exporter ma sauvegarde", { exporter.launch("MetaHuman-Legacy-sauvegarde.json") }, Modifier.fillMaxWidth(), enabled = !busy)
        Spacer(Modifier.height(8.dp))
        if (!busy) MhlSecondaryButton("Importer une sauvegarde", { importer.launch("application/json") }, Modifier.fillMaxWidth())
        notice?.let { Spacer(Modifier.height(10.dp)); Text(it, color = UltimateIvory, fontSize = 13.sp, lineHeight = 20.sp) }
    }
}
