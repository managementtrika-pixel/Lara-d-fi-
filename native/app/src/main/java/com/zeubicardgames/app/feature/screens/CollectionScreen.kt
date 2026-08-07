package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.core.model.CardType
import com.zeubicardgames.app.core.model.Rarity
import com.zeubicardgames.app.feature.shell.*

private enum class OwnershipFilter(val label: String) {
    ALL("Toutes"),
    OWNED("Possédées"),
    MISSING("Manquantes"),
    FAVORITES("★ Favoris"),
}

@Composable
fun CollectionScreen(state: GameUiState, vm: GameViewModel) {
    var query by remember { mutableStateOf("") }
    var setId by remember { mutableStateOf("all") }
    var ownership by remember { mutableStateOf(OwnershipFilter.ALL) }
    var rarity by remember { mutableStateOf<Rarity?>(null) }
    var type by remember { mutableStateOf<CardType?>(null) }

    val favoriteIds = state.preferences.favoriteCardIds
    val ownedUnique = state.cards.count { (state.owned[it.canonicalId]?.quantity ?: 0) > 0 }
    val visibleRarities = state.cards.map { it.rarity }.distinct().sortedBy { it.rank }
    val visibleTypes = state.cards.map { it.type }.filter { it != CardType.INCONNU }.distinct()

    val cards = state.cards.filter { card ->
        val quantity = state.owned[card.canonicalId]?.quantity ?: 0
        val discovered = quantity > 0
        val queryMatches = query.isBlank() || card.number.contains(query, ignoreCase = true) ||
            (discovered && card.name.contains(query, ignoreCase = true))
        val setMatches = setId == "all" || card.setId == setId
        val ownershipMatches = when (ownership) {
            OwnershipFilter.ALL -> true
            OwnershipFilter.OWNED -> discovered
            OwnershipFilter.MISSING -> !discovered
            OwnershipFilter.FAVORITES -> discovered && card.canonicalId in favoriteIds
        }
        val rarityMatches = rarity == null || (discovered && card.rarity == rarity)
        val typeMatches = type == null || (discovered && card.type == type)

        queryMatches && setMatches && ownershipMatches && rarityMatches && typeMatches
    }

    val selectedExtension = state.extensions.firstOrNull { it.id == setId }
    val selectedSetCards = if (setId == "all") state.cards else state.cards.filter { it.setId == setId }
    val selectedSetOwned = selectedSetCards.count { (state.owned[it.canonicalId]?.quantity ?: 0) > 0 }

    Column(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
        Text("Cartodex $ownedUnique/${state.cards.size}", style = MaterialTheme.typography.headlineSmall)
        if (selectedExtension != null) {
            Text(
                "${selectedExtension.name} · $selectedSetOwned/${selectedSetCards.size}",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Rechercher une carte découverte ou un numéro") },
            singleLine = true,
        )

        FilterRow {
            FilterChip(
                selected = setId == "all",
                onClick = { setId = "all" },
                label = { Text("Toutes extensions") },
            )
            state.extensions.forEach { extension ->
                FilterChip(
                    selected = setId == extension.id,
                    onClick = { setId = extension.id },
                    label = { Text(extension.name) },
                )
            }
        }

        FilterRow {
            OwnershipFilter.entries.forEach { filter ->
                FilterChip(
                    selected = ownership == filter,
                    onClick = { ownership = filter },
                    label = { Text(filter.label) },
                )
            }
        }

        FilterRow {
            FilterChip(
                selected = rarity == null,
                onClick = { rarity = null },
                label = { Text("Toutes raretés") },
            )
            visibleRarities.forEach { value ->
                FilterChip(
                    selected = rarity == value,
                    onClick = { rarity = value },
                    label = { Text(value.label) },
                )
            }
        }

        FilterRow {
            FilterChip(
                selected = type == null,
                onClick = { type = null },
                label = { Text("Tous types") },
            )
            visibleTypes.forEach { value ->
                FilterChip(
                    selected = type == value,
                    onClick = { type = value },
                    label = { Text(value.label) },
                )
            }
        }

        Text(
            "${cards.size} carte${if (cards.size > 1) "s" else ""} affichée${if (cards.size > 1) "s" else ""}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            contentPadding = PaddingValues(vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(cards, key = { it.canonicalId }) { card ->
                val quantity = state.owned[card.canonicalId]?.quantity ?: 0
                CardTile(
                    card = card,
                    quantity = quantity,
                    onClick = { vm.inspect(card) },
                    favorite = card.canonicalId in favoriteIds,
                    enabled = quantity > 0,
                )
            }
        }
    }
}

@Composable
private fun FilterRow(content: @Composable RowScope.() -> Unit) {
    Row(
        Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}
