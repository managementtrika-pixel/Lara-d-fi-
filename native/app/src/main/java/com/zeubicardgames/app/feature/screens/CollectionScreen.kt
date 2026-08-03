package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.feature.shell.*

@Composable fun CollectionScreen(state: GameUiState, vm: GameViewModel) {
    var query by remember { mutableStateOf("") }; var set by remember { mutableStateOf("all") }
    val cards = state.cards.filter { (set == "all" || it.setId == set) && it.name.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Text("Cartodex ${state.owned.size}/${state.cards.size}", style = MaterialTheme.typography.headlineSmall)
        OutlinedTextField(query, { query = it }, Modifier.fillMaxWidth(), placeholder = { Text("Rechercher") }, singleLine = true)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) { FilterChip(set == "all", { set = "all" }, { Text("Tout") }); state.extensions.forEach { e -> FilterChip(set == e.id, { set = e.id }, { Text(e.name.take(8)) }) } }
        LazyVerticalGrid(GridCells.Fixed(3), contentPadding = PaddingValues(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(cards, key = { it.canonicalId }) { card -> CardTile(card, state.owned[card.canonicalId]?.quantity ?: 0, { vm.inspect(card) }) }
        }
    }
}
