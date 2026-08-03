package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.core.designsystem.AssetImage
import com.zeubicardgames.app.core.gameengine.BattlePhase
import com.zeubicardgames.app.core.model.OfficialOpponents
import com.zeubicardgames.app.feature.shell.*

@Composable fun BattleHubScreen(state: GameUiState, vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Combat", style = MaterialTheme.typography.headlineMedium)
        PremiumCard(Modifier.fillMaxWidth().clickable { vm.overlay(OverlayScreen.Campaign) }) { Text("Combat hors ligne", style = MaterialTheme.typography.titleLarge); Text("Campagne IA · 8 duels"); Button(onClick = { vm.overlay(OverlayScreen.Campaign) }) { Text("CHOISIR UN DUEL") } }
        PremiumCard(Modifier.fillMaxWidth()) { Text("Combat en ligne", style = MaterialTheme.typography.titleLarge); Text("BIENTÔT DISPONIBLE", color = MaterialTheme.colorScheme.primary); Button(onClick = {}, enabled = false) { Text("VERROUILLÉ") } }
    }
}

@Composable fun CampaignScreen(state: GameUiState, vm: GameViewModel) {
    Column(Modifier.fillMaxSize().padding(12.dp)) { Text("Campagne hors ligne", style = MaterialTheme.typography.headlineSmall); LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) { items(OfficialOpponents, key = { it.id }) { op ->
        val done = state.campaign[op.id]?.completed == true
        PremiumCard(Modifier.fillMaxWidth().clickable { vm.chooseOpponent(op) }) { Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Column(Modifier.weight(1f)) { Text(op.name, style = MaterialTheme.typography.titleMedium); Text("${op.difficulty} · ${op.rewardCoins} ◈"); Text(op.description, color = MaterialTheme.colorScheme.onSurfaceVariant) }; if (done) Text("✓ TERMINÉ", color = Color(0xFF2D9A67)) }; Button(onClick = { vm.chooseOpponent(op); vm.startDuel() }) { Text("AFFRONTER") } }
    } } }
}

@Composable fun DuelScreen(state: GameUiState, vm: GameViewModel) {
    val battle = state.battle ?: return
    val player = battle.player
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Text("${state.selectedOpponent?.name ?: "Adversaire"} · ${battle.opponentPoints}/3", style = MaterialTheme.typography.titleMedium)
        FighterPanel("ADVERSAIRE", battle.opponent?.card?.name, battle.opponent?.card?.variants?.firstOrNull()?.fullPath, battle.opponent?.hpLeft ?: 0, battle.opponent?.card?.hp ?: 0)
        HorizontalDivider(); Text(when (battle.phase) { BattlePhase.AiTurn -> "TOUR IA"; BattlePhase.GameOver -> if (battle.winner == "player") "VICTOIRE" else "DÉFAITE"; else -> "À TOI DE JOUER" }, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.align(Alignment.CenterHorizontally))
        FighterPanel("TON ACTIF", battle.player?.card?.name, battle.player?.card?.variants?.firstOrNull()?.fullPath, battle.player?.hpLeft ?: 0, battle.player?.card?.hp ?: 0)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { Button(onClick = vm::attachEnergy, enabled = battle.phase == BattlePhase.PlayerMain && !battle.energyAttachedThisTurn, modifier = Modifier.weight(1f)) { Text("ÉNERGIE +1") }; player?.card?.attacks?.firstOrNull()?.let { a -> Button(onClick = { vm.attack(a) }, enabled = battle.phase == BattlePhase.PlayerMain && (player.energy >= a.cost), modifier = Modifier.weight(1f)) { Text("ATTAQUER ${a.damage}") } } }
        Text(battle.events.takeLast(3).joinToString(" · ") { it.message }, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable private fun FighterPanel(label: String, name: String?, path: String?, hp: Int, max: Int) { PremiumCard(Modifier.fillMaxWidth()) { Text(label, style = MaterialTheme.typography.labelLarge); Row(verticalAlignment = Alignment.CenterVertically) { AssetImage(path, name, Modifier.width(95.dp).height(130.dp), ContentScale.Fit); Column(Modifier.weight(1f)) { Text(name ?: "—", style = MaterialTheme.typography.titleLarge); Text("PV $hp/$max"); LinearProgressIndicator(progress = { if (max == 0) 0f else hp.toFloat()/max }, Modifier.fillMaxWidth()) } } } }
