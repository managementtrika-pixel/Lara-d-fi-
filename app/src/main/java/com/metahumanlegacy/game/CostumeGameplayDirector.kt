package com.metahumanlegacy.game

internal data class CostumeUpdate(
    val campaign: Campaign,
    val ultimate: UltimateState,
    val deep: DeepLifeState,
    val echo: String = ""
)

/**
 * Costume choices now trade secrecy, protection, mobility and public readability.
 * The visual identity remains the same PixelAvatar; equipment changes consequences, not identity.
 */
internal object CostumeGameplayDirector {
    fun afterChoice(c: Campaign, u: UltimateState, d: DeepLifeState, event: EventNode, choice: Choice): CostumeUpdate {
        if (!c.powerRevealed || u.costumeEra <= 0) return CostumeUpdate(c, u, d)
        var campaign = c
        var deep = d
        val echo = mutableListOf<String>()

        val concealment = when (u.maskStyle) {
            "Masque intégral", "Casque" -> 3
            "Demi-masque", "Capuche", "Visière" -> 2
            "Masque minimal" -> 1
            else -> 0
        }
        val visibility = when (u.heroPresentation) {
            "Flamboyant" -> 3
            "Institutionnel" -> 2
            "Intimidant" -> 2
            "Clandestin", "Mystérieux" -> -1
            else -> 0
        }
        val protection = when (u.heroPresentation) {
            "Tactique" -> 2
            "Institutionnel" -> 1
            else -> 0
        }

        if (choice.identityDelta > 0 || event.category.contains("IDENT", true) || event.category.contains("MÉDIA", true)) {
            val reduced = (campaign.identityExposure - concealment + visibility.coerceAtLeast(0)).coerceIn(0, 100)
            if (reduced != campaign.identityExposure) {
                campaign = campaign.copy(identityExposure = reduced)
                if (concealment > 0) echo += "Ton ${u.maskStyle.lowercase()} brouille une partie des indices visuels laissés par l'intervention."
                if (visibility > 1) echo += "Ton identité visuelle est très reconnaissable : le public retient facilement le symbole, même quand il ignore encore le visage."
            }
        }

        if (protection > 0 && campaign.health < c.health) {
            campaign = campaign.copy(health = (campaign.health + protection).coerceAtMost(100))
            echo += "La conception ${u.heroPresentation.lowercase()} du costume absorbe une partie du coût physique de la scène."
        }

        var perception = deep.perception
        when (u.heroPresentation) {
            "Flamboyant" -> perception = perception.copy(
                city = (perception.city + 1).coerceAtMost(100),
                youth = (perception.youth + 2).coerceAtMost(100)
            )
            "Intimidant" -> perception = perception.copy(
                criminalFear = (perception.criminalFear + 2).coerceAtMost(100),
                civilianFear = (perception.civilianFear + 1).coerceAtMost(100)
            )
            "Institutionnel" -> perception = perception.copy(
                government = (perception.government + 1).coerceAtMost(100),
                police = (perception.police + 1).coerceAtMost(100)
            )
            "Clandestin", "Mystérieux" -> perception = perception.copy(
                city = (perception.city - 1).coerceAtLeast(-100)
            )
        }
        deep = deep.copy(perception = perception)
        return CostumeUpdate(campaign, u, deep, echo.distinct().joinToString("\n"))
    }
}
