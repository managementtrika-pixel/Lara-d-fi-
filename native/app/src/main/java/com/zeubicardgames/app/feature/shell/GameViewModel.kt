package com.zeubicardgames.app.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zeubicardgames.app.core.data.*
import com.zeubicardgames.app.core.database.CampaignEntity
import com.zeubicardgames.app.core.gameengine.*
import com.zeubicardgames.app.core.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class MainTab { HOME, COLLECTION, OPEN, BATTLE, MENU }
sealed interface OverlayScreen { data object None : OverlayScreen; data object Decks : OverlayScreen; data object Missions : OverlayScreen; data object Settings : OverlayScreen; data object Campaign : OverlayScreen; data object Duel : OverlayScreen }

data class GameUiState(
    val tab: MainTab = MainTab.HOME,
    val overlay: OverlayScreen = OverlayScreen.None,
    val cards: List<CardDefinition> = emptyList(),
    val extensions: List<ExtensionDefinition> = emptyList(),
    val owned: Map<String, OwnedCard> = emptyMap(),
    val decks: List<Deck> = emptyList(),
    val campaign: Map<String, CampaignEntity> = emptyMap(),
    val preferences: UserPreferences = UserPreferences(),
    val selectedExtension: String = "ninja",
    val packResult: List<CardDefinition> = emptyList(),
    val pendingPackId: String? = null,
    val packRecovered: Boolean = false,
    val openingPack: Boolean = false,
    val selectedCard: CardDefinition? = null,
    val selectedOpponent: CampaignOpponent? = null,
    val battle: BattleState? = null,
    val notice: String? = null,
)

@HiltViewModel
class GameViewModel @Inject constructor(
    private val repository: GameRepository,
    private val preferences: PreferencesStore,
) : ViewModel() {
    private val local = MutableStateFlow(GameUiState(cards = repository.catalog, extensions = repository.extensions))
    val state: StateFlow<GameUiState> = combine(
        local,
        repository.owned,
        repository.decks,
        repository.campaign,
        preferences.flow,
    ) { a, owned, decks, campaign, prefs ->
        a.copy(owned = owned, decks = decks, campaign = campaign, preferences = prefs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), local.value)

    private var engine: BattleEngine? = null

    init {
        viewModelScope.launch {
            repository.pendingPack()?.let { opening ->
                local.update {
                    it.copy(
                        selectedExtension = opening.setId,
                        packResult = opening.cards,
                        pendingPackId = opening.id,
                        packRecovered = true,
                    )
                }
            }
        }
    }

    fun tab(tab: MainTab) {
        local.update { it.copy(tab = tab, overlay = OverlayScreen.None, selectedCard = null) }
    }

    fun overlay(screen: OverlayScreen) { local.update { it.copy(overlay = screen) } }
    fun dismissOverlay() { local.update { it.copy(overlay = OverlayScreen.None, selectedCard = null) } }

    fun chooseExtension(id: String) {
        if (local.value.pendingPackId != null) return
        local.update { it.copy(selectedExtension = id, packResult = emptyList(), packRecovered = false) }
    }

    fun inspect(card: CardDefinition?) { local.update { it.copy(selectedCard = card) } }

    fun openPack() = viewModelScope.launch { openPackInternal() }

    fun openAnotherPack() = viewModelScope.launch {
        acknowledgeCurrentPack()
        openPackInternal()
    }

    fun finishPack() = viewModelScope.launch {
        acknowledgeCurrentPack()
        local.update { it.copy(packResult = emptyList(), packRecovered = false) }
    }

    private suspend fun acknowledgeCurrentPack() {
        local.value.pendingPackId?.let(repository::acknowledgePack)
        local.update { it.copy(pendingPackId = null, packResult = emptyList(), packRecovered = false) }
    }

    private suspend fun openPackInternal() {
        if (local.value.openingPack) return
        local.update { it.copy(openingPack = true, notice = null) }
        val opening = repository.openPack(local.value.selectedExtension)
        if (opening == null) {
            local.update { it.copy(openingPack = false, notice = "Aucune carte disponible pour cette extension") }
            return
        }
        local.update {
            it.copy(
                selectedExtension = opening.setId,
                packResult = opening.cards,
                pendingPackId = opening.id,
                packRecovered = opening.recovered,
                openingPack = false,
            )
        }
    }

    fun selectVariant(cardId: String, variantId: String) = viewModelScope.launch {
        repository.selectVariant(cardId, variantId)
    }

    fun chooseOpponent(opponent: CampaignOpponent) { local.update { it.copy(selectedOpponent = opponent) } }

    fun startDuel() {
        val s = local.value
        val opponent = s.selectedOpponent ?: return
        val ownedCard = s.cards.firstOrNull {
            (s.owned[it.canonicalId]?.quantity ?: 0) > 0 && it.kind == "pokemon" && it.stage == "base"
        } ?: s.cards.firstOrNull { it.kind == "pokemon" && it.stage == "base" }
        val foe = s.cards.firstOrNull { it.name == opponent.bossCardName }
            ?: s.cards.firstOrNull { it.setId == opponent.extensionId && it.kind == "pokemon" }
            ?: return
        if (ownedCard == null) return
        engine = BattleEngine(System.currentTimeMillis())
        local.update { it.copy(overlay = OverlayScreen.Duel, battle = engine!!.start(ownedCard, foe)) }
    }

    fun attachEnergy() {
        local.update { it.copy(battle = it.battle?.let { b -> engine?.attachEnergy(b) }) }
    }

    fun attack(attack: Attack) {
        local.update { it.copy(battle = it.battle?.let { b -> engine?.attack(b, attack) }) }
    }

    fun toggleSound(v: Boolean) = viewModelScope.launch { preferences.setSound(v) }
    fun toggleHaptics(v: Boolean) = viewModelScope.launch { preferences.setHaptics(v) }
    fun toggleReduced(v: Boolean) = viewModelScope.launch { preferences.setReducedMotion(v) }
}
