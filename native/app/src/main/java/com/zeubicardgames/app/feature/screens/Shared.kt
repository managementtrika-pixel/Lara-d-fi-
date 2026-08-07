package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.core.designsystem.*
import com.zeubicardgames.app.core.model.*

@Composable
fun PremiumCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) = Surface(
    modifier,
    shape = RoundedCornerShape(24.dp),
    tonalElevation = 2.dp,
    shadowElevation = 4.dp,
) {
    Column(Modifier.padding(16.dp), content = content)
}

@Composable
fun CardTile(
    card: CardDefinition,
    quantity: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    favorite: Boolean = false,
    enabled: Boolean = true,
) {
    val discovered = quantity > 0
    val border = if (!discovered) {
        Color.Transparent
    } else {
        when (card.rarity) {
            Rarity.SUPRA, Rarity.UR -> ZeubiGold
            Rarity.SR -> ZeubiViolet
            Rarity.R -> Color(0xFF4A90B8)
            else -> Color.Transparent
        }
    }

    Surface(
        modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, border),
        tonalElevation = 1.dp,
    ) {
        Column(Modifier.padding(7.dp)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(5f / 7f).clip(RoundedCornerShape(13.dp))
            ) {
                AssetImage(
                    card.variants.firstOrNull()?.thumbPath,
                    if (discovered) card.name else "Carte non découverte",
                    Modifier.fillMaxSize(),
                    ContentScale.Fit,
                )
                if (!discovered) {
                    Box(
                        Modifier.fillMaxSize().background(Color(0xCC1C2431)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("?", color = Color.White, style = MaterialTheme.typography.headlineLarge)
                    }
                } else if (favorite) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopEnd).padding(6.dp),
                        shape = RoundedCornerShape(999.dp),
                        color = Color(0xCC11151C),
                    ) {
                        Text(
                            "★",
                            color = ZeubiGold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(
                if (discovered) card.name else "Inconnue",
                maxLines = 1,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                if (discovered) "${card.number} · ${card.rarity.label}" else "${card.number} · Non découverte",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun CardDetailDialog(
    card: CardDefinition,
    owned: OwnedCard?,
    favorite: Boolean,
    dismiss: (CardDefinition?) -> Unit,
    select: (String, String) -> Unit,
    toggleFavorite: (String) -> Unit,
) {
    val quantity = owned?.quantity ?: 0
    AlertDialog(
        onDismissRequest = { dismiss(null) },
        confirmButton = {
            TextButton(onClick = { dismiss(null) }) { Text("Fermer") }
        },
        dismissButton = {
            if (quantity > 0) {
                TextButton(onClick = { toggleFavorite(card.canonicalId) }) {
                    Text(if (favorite) "★ Retirer des favoris" else "☆ Ajouter aux favoris")
                }
            }
        },
        title = { Text(card.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AssetImage(
                    card.variants.firstOrNull { it.variantId == owned?.selectedVariantId }?.fullPath
                        ?: card.variants.firstOrNull()?.fullPath,
                    card.name,
                    Modifier.fillMaxWidth().aspectRatio(5f / 7f),
                )
                Text(
                    "${card.setId.uppercase()} · ${card.number} · ${card.rarity.label} · x$quantity",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    buildString {
                        append(card.type.label)
                        if (card.type == CardType.PERSONNAGE) {
                            append(" · ")
                            append(card.evolutionStage.label)
                        }
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (card.type == CardType.PERSONNAGE) {
                    Text("PV ${card.hp} · retraite ${card.retreat}")
                    card.evolvesFrom?.let { Text("Évolue depuis : $it") }
                }
                card.attacks.forEach { attack ->
                    Text("${attack.name} — ${attack.damage} dégâts · coût ${attack.cost}")
                }
                card.effect?.let { effect ->
                    Text(effect, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (card.variants.size > 1 && quantity > 0) {
                    Text("Illustration", style = MaterialTheme.typography.labelLarge)
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        card.variants.forEach { variant ->
                            FilterChip(
                                selected = owned?.selectedVariantId == variant.variantId,
                                onClick = { select(card.canonicalId, variant.variantId) },
                                label = { Text(variant.variantId.substringAfterLast(':')) },
                            )
                        }
                    }
                }
            }
        },
    )
}
