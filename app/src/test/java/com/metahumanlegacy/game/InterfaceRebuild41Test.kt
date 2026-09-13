package com.metahumanlegacy.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterfaceRebuild41Test {
    @Test fun touchTargetsMeetComfortFloor() {
        assertTrue(Interface41.minTouchDp >= 48)
    }

    @Test fun narrativeSceneRemainsPrimary() {
        assertTrue(Interface41.narrativeSceneRatio() >= .55f)
    }

    @Test fun compactDockKeepsAllCoreDestinations() {
        assertEquals(
            listOf("DESTIN", "ACTIONS", "PERSONNAGE", "VILLE", "LIENS", "CHRONIQUE"),
            Interface41.compactNavIds()
        )
    }
}
