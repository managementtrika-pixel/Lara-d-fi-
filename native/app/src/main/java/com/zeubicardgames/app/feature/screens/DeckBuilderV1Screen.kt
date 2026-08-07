package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.core.deck.DeckRules
import com.zeubicardgames.app.core.model.CardDefinition
import com.zeubicardgames.app.core.model.Deck
import com.zeubicardgames.app.feature.shell.GameUiState
import com.zeubicardgames.app.feature.shell.GameViewModel

@Composable
fun DeckBuilderV1Screen(state: GameUiState, vm: GameViewModel) {
    var editingId by remember { mutableStateOf<Long?>(null) }
    var draftName by remember { mutableStateOf("") }
    var draftIds by remember { mutableStateOf<List<String>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var pendingDelete by remember { mutableStateOf<Deck?>(null) }

    val byId = remember(state.cards) { state.cards.associateBy { it.canonicalId } }

    fun openNew() {
        vm.clearDeckNotice()
        editingId = 0L
        draftName = "Deck PLAYER"
        draftIds = emptyList()
        query = ""
    }

    fun openDeck(deck: Deck) {
        vm.clearDeckNotice()
        editingId = deck.id
        draftName = deck.name
        draftIds = deck.cardIds
        query = ""
    }

    fun closeEditor() {
        vm.clearDeckNotice()
        editingId = null
        draftName = ""
        draftIds = emptyList()
        query = ""
    }

    pendingDelete?.let { deck ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Supprimer ${deck.name} ?") },
            text = { Text("Cette action supprime uniquement ce deck. Les cartes restent dans ta collection.") },
            confirmButton = {
                Button(
                    onClick = {
                        vm.deleteDeck(deck.id)
                        pendingDelete = null
                    }
                ) { Text("SUPPRIMER") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Annuler") }
            },
        )
    }

    if (editingId == null) {
        DeckList(
            state = state,
            byId = byId,
            onNew = ::openNew,
            onEdit = ::openDeck,
            onDuplicate = vm::duplicateDeck,
            onDelete = { pendingDelete = it },
            onClose = vm::dismissOverlay,
        )
    } else {
        DeckEditor(
            state = state,
            vm = vm,
            deckId = editingId ?: 0L,
            name = draftName,
            ids = draftIds,
            query = query,
            onName = { draftName = it },
            onQuery = { query = it },
            onIds = { draftIds = it },
            onBack = ::closeEditor,
        )
    }
}

@Composable
private fun DeckList(
    state: GameUiState,
    byId: Map<String, CardDefinition>,
    onNew: () -> Unit,
    onEdit: (Deck) -> Unit,
    onDuplicate: (Deck) -> Unit,
    onDelete: (Deck) -> Unit,
    onClose: () -> Unit,
) {
    Column(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("Mes decks", style = MaterialTheme.typography.headlineSmall)
                Text(
                    "${state.decks.size} deck${if (state.decks.size > 1) "s" else ""}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onClose) { Text("Fermer") }
        }

        Button(onClick = onNew, modifier = Modifier.fillMaxWidth()) {
            Text("NOUVEAU DECK")
        }

        state.deckNotice?.let { DeckNotice(it) }

        if (state.decks.isEmpty()) {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text("Aucun deck enregistré", fontWeight = FontWeight.Bold)
                Text(
                    "Crée un deck avec les cartes déjà présentes dans ta collection.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
            ) {
                items(state.decks, key = { it.id }) { deck ->
                    val validation = remember(deck.cardIds, state.cards, state.owned) {
                        val owned = state.owned.mapValues { it.value.quantity }
                        com.zeubicardgames.app.core.deck.DeckRules.validate(deck.cardIds, state.cards, owned)
                    }
                    PremiumCard(Modifier.fillMaxWidth()) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(deck.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${deck.cardIds.size}/${DeckRules.DECK_SIZE} cartes · ${deck.cardIds.distinct().size} différentes",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                val characters = deck.cardIds.mapNotNull(byId::get)
                                    .count { it.type.label == "Personnage" }
                                Text(
                                    "$characters Personnage${if (characters > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                )
                                Text(
                                    if (validation.isValid) "Deck valide" else validation.errors.first(),
                                    color = if (validation.isValid) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium,
                                )
                            }
                        }
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Button(onClick = { onEdit(deck) }, modifier = Modifier.weight(1f)) {
                                Text("MODIFIER")
                            }
                            OutlinedButton(
                                onClick = { onDuplicate(deck) },
                                enabled = !state.deckBusy,
                                modifier = Modifier.weight(1f),
                            ) { Text("DUPLIQUER") }
                        }
                        TextButton(onClick = { onDelete(deck) }) { Text("Supprimer") }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeckEditor(
    state: GameUiState,
    vm: GameViewModel,
    deckId: Long,
    name: String,
    ids: List<String>,
    query: String,
    onName: (String) -> Unit,
    onQuery: (String) -> Unit,
    onIds: (List<String>) -> Unit,
    onBack: () -> Unit,
) {
    val byId = remember(state.cards) { state.cards.associateBy { it.canonicalId } }
    val ownedQuantities = remember(state.owned) { state.owned.mapValues { it.value.quantity } }
    val validation = remember(ids, state.cards, ownedQuantities) {
        DeckRules.validate(ids, state.cards, ownedQuantities)
    }
    val selectedCounts = remember(ids) { ids.groupingBy { it }.eachCount() }
    val selectedCards = selectedCounts.keys.mapNotNull(byId::get).sortedWith(
        compareBy<CardDefinition>({ it.setId }, { it.number })
    )
    val availableCards = state.cards.filter { card ->
        val owned = ownedQuantities[card.canonicalId] ?: 0
        owned > 0 && (query.isBlank() || card.name.contains(query, true) || card.number.contains(query, true))
    }.sortedWith(compareBy<CardDefinition>({ it.setId }, { it.number }))

    fun add(card: CardDefinition) {
        val current = selectedCounts[card.canonicalId] ?: 0
        val owned = ownedQuantities[card.canonicalId] ?: 0
        val maxAllowed = minOf(DeckRules.MAX_COPIES_PER_CARD, owned)
        if (ids.size < DeckRules.DECK_SIZE && current < maxAllowed) {
            onIds(ids + card.canonicalId)
            vm.clearDeckNotice()
        }
    }

    fun remove(cardId: String) {
        val index = ids.indexOfLast { it == cardId }
        if (index >= 0) {
            onIds(ids.toMutableList().also { it.removeAt(index) })
            vm.clearDeckNotice()
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        if (deckId == 0L) "Nouveau deck" else "Modifier le deck",
                        style = MaterialTheme.typography.headlineSmall,
                    )
                    Text(
                        "${ids.size}/${DeckRules.DECK_SIZE} cartes",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onBack) { Text("Retour") }
            }
        }

        item {
            OutlinedTextField(
                value = name,
                onValueChange = {
                    onName(it)
                    vm.clearDeckNotice()
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nom du deck") },
                singleLine = true,
            )
        }

        item {
            PremiumCard(Modifier.fillMaxWidth()) {
                Text(
                    if (validation.isValid && name.isNotBlank()) "Deck valide" else "À corriger",
                    fontWeight = FontWeight.Bold,
                    color = if (validation.isValid && name.isNotBlank()) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error,
                )
                if (name.isBlank()) Text("• Donne un nom au deck.")
                validation.errors.forEach { Text("• $it") }
                if (validation.isValid && name.isNotBlank()) {
                    Text(
                        "Le deck respecte les règles actuellement actives et les quantités de ta collection.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        item {
            Button(
                onClick = { vm.saveDeck(deckId, name, ids) },
                enabled = validation.isValid && name.isNotBlank() && !state.deckBusy,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (state.deckBusy) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (deckId == 0L) "ENREGISTRER LE DECK" else "ENREGISTRER LES MODIFICATIONS")
            }
            state.deckNotice?.let { DeckNotice(it) }
        }

        item {
            HorizontalDivider()
            Text("Cartes du deck", style = MaterialTheme.typography.titleLarge)
            if (selectedCards.isEmpty()) {
                Text(
                    "Ajoute des cartes depuis ta collection ci-dessous.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        items(selectedCards, key = { "selected-${it.canonicalId}" }) { card ->
            val selected = selectedCounts[card.canonicalId] ?: 0
            val owned = ownedQuantities[card.canonicalId] ?: 0
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(card.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${card.type.label} · ${card.rarity.label} · x$selected dans le deck / x$owned possédé",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(onClick = { remove(card.canonicalId) }) { Text("−") }
                    FilledTonalButton(
                        onClick = { add(card) },
                        enabled = selected < minOf(DeckRules.MAX_COPIES_PER_CARD, owned) && ids.size < DeckRules.DECK_SIZE,
                    ) { Text("+") }
                }
            }
        }

        item {
            HorizontalDivider()
            Text("Ma collection", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = query,
                onValueChange = onQuery,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Rechercher une carte possédée") },
                singleLine = true,
            )
        }

        items(availableCards, key = { "available-${it.canonicalId}" }) { card ->
            val selected = selectedCounts[card.canonicalId] ?: 0
            val owned = ownedQuantities[card.canonicalId] ?: 0
            val maxAllowed = minOf(DeckRules.MAX_COPIES_PER_CARD, owned)
            Card {
                Row(
                    Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(card.name, fontWeight = FontWeight.Bold)
                        Text(
                            "${card.setId.uppercase()} ${card.number} · ${card.type.label} · ${card.rarity.label}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Deck x$selected/$maxAllowed · Collection x$owned",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                    TextButton(onClick = { vm.inspect(card) }) { Text("Voir") }
                    Button(
                        onClick = { add(card) },
                        enabled = ids.size < DeckRules.DECK_SIZE && selected < maxAllowed,
                    ) { Text("+") }
                }
            }
        }

        if (availableCards.isEmpty()) {
            item {
                Text(
                    if (query.isBlank()) "Aucune carte possédée disponible."
                    else "Aucune carte possédée ne correspond à cette recherche.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DeckNotice(message: String) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(message, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
    }
}
