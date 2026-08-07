package com.zeubicardgames.app.core.effects

enum class EffectFamily {
    SEARCH,
    DRAW,
    RESOURCE,
    SWITCH,
    HEAL,
    BUFF,
    EVOLUTION,
    DEFENSE,
    COUNTER,
    CANCEL,
    RECOVERY,
}

enum class KnownCardEffect(
    val wireCode: String,
    val family: EffectFamily,
) {
    SEARCH_PERSONNAGE("searchPokemon", EffectFamily.SEARCH),
    RECOVER_ENERGY_2("recoverEnergy2", EffectFamily.RESOURCE),
    SWITCH_AND_BUFF("switchAndBuff", EffectFamily.SWITCH),
    PEEK_HAND_DRAW("peekHandDraw", EffectFamily.DRAW),
    STREAK_DRAW("streakDraw", EffectFamily.DRAW),
    SWITCH_HEAL_30("switchHeal30", EffectFamily.SWITCH),
    REDUCE_40("reduce40", EffectFamily.DEFENSE),
    REDUCE_30_LOCK("reduce30Lock", EffectFamily.DEFENSE),
    COUNTER_30("counter30", EffectFamily.COUNTER),
    SURVIVE_10("survive10", EffectFamily.DEFENSE),
    CANCEL_ACTION("cancelTrainer", EffectFamily.CANCEL),
    REVIVE_60("revive60", EffectFamily.RECOVERY),
    HEAL_90("heal90", EffectFamily.HEAL),
    EFFECT_ENERGY("effectEnergy", EffectFamily.RESOURCE),
    BUFF_30_SELF_10("buff30Self10", EffectFamily.BUFF),
    INSTANT_EVOLVE("instantEvolve", EffectFamily.EVOLUTION),
    BUFF_50("buff50", EffectFamily.BUFF),
    SWITCH_BEFORE_HIT("switchBeforeHit", EffectFamily.SWITCH),
    EVOLVE_SURVIVE_30("evolveSurvive30", EffectFamily.EVOLUTION),
    CANCEL_BONUS("cancelBonus", EffectFamily.CANCEL),
    DRAW_2_EXTRA_ENERGY("draw2ExtraEnergy", EffectFamily.RESOURCE),
    RECOVER_2("recover2", EffectFamily.RECOVERY),
    SURVIVE_10_BUFF_50("survive10Buff50", EffectFamily.DEFENSE),
    SEARCH_ACTION_DRAW("searchTrainerDraw", EffectFamily.SEARCH),
    SWITCH("switch", EffectFamily.SWITCH),
    SEARCH_EVOLUTION("searchEvolution", EffectFamily.SEARCH),
    MOVE_ENERGY("moveEnergy", EffectFamily.RESOURCE),
    DRAW_3_DISCARD_1("draw3discard1", EffectFamily.DRAW),
    SHIELD_40("shield40", EffectFamily.DEFENSE),
    ;

    companion object {
        private val byWireCode = entries.associateBy(KnownCardEffect::wireCode)

        fun from(raw: String?): KnownCardEffect? = raw?.let(byWireCode::get)
        fun isKnown(raw: String?): Boolean = raw == null || raw in byWireCode
        val wireCodes: Set<String> = byWireCode.keys
    }
}
