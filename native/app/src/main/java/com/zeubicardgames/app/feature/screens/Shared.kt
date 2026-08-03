package com.zeubicardgames.app.feature.screens

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zeubicardgames.app.core.designsystem.*
import com.zeubicardgames.app.core.model.*

@Composable fun PremiumCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) = Surface(modifier, shape = RoundedCornerShape(24.dp), tonalElevation = 2.dp, shadowElevation = 4.dp) { Column(Modifier.padding(16.dp), content = content) }

@Composable fun CardTile(card: CardDefinition, quantity: Int, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border = when (card.rarity) { Rarity.UR -> ZeubiGold; Rarity.SR -> ZeubiViolet; Rarity.R -> Color(0xFF4A90B8); else -> Color.Transparent }
    Surface(modifier.clickable(onClick = onClick), shape = RoundedCornerShape(18.dp), border = BorderStroke(1.dp, border), tonalElevation = 1.dp) {
        Column(Modifier.padding(7.dp)) {
            Box(Modifier.fillMaxWidth().aspectRatio(5f/7f).clip(RoundedCornerShape(13.dp))) {
                AssetImage(card.variants.firstOrNull()?.thumbPath, card.name, Modifier.fillMaxSize(), ContentScale.Fit)
                if (quantity <= 0) Box(Modifier.fillMaxSize().background(Color(0xAA1C2431)), contentAlignment = Alignment.Center) { Text("?", color = Color.White, style = MaterialTheme.typography.headlineLarge) }
            }
            Spacer(Modifier.height(6.dp)); Text(if (quantity > 0) card.name else "Inconnue", maxLines = 1, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Text("${card.number} · ${card.rarity}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable fun CardDetailDialog(card: CardDefinition, owned: OwnedCard?, dismiss: (CardDefinition?) -> Unit, select: (String, String) -> Unit) {
    AlertDialog(onDismissRequest = { dismiss(null) }, confirmButton = { TextButton(onClick = { dismiss(null) }) { Text("Fermer") } }, title = { Text(card.name) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            AssetImage(card.variants.firstOrNull { it.variantId == owned?.selectedVariantId }?.fullPath ?: card.variants.firstOrNull()?.fullPath, card.name, Modifier.fillMaxWidth().aspectRatio(5f/7f))
            Text("${card.setId} · ${card.number} · ${card.rarity} · x${owned?.quantity ?: 0}")
            card.attacks.forEach { Text("${it.name} — ${it.damage} dégâts · coût ${it.cost}") }
            if (card.variants.size > 1) Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) { card.variants.forEach { v -> FilterChip(selected = owned?.selectedVariantId == v.variantId, onClick = { select(card.canonicalId, v.variantId) }, label = { Text(v.variantId.substringAfterLast(':')) }) } }
        }
    })
}
