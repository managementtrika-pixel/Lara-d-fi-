package com.metahumanlegacy.game

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VisualAssetPolicy45Test {
    @Test fun reviewedSourcesAreSafeForCommercialBuilds() {
        assertTrue(VisualAssetPolicy45.reviewedSources.isNotEmpty())
        assertTrue(VisualAssetPolicy45.reviewedSources.all(VisualAssetPolicy45::canShip))
    }

    @Test fun attributionLicensesDoNotPassTheZeroFrictionGate() {
        val by = VisualAssetSource("x", "x", "x", VisualAssetLicense.CC_BY, true, true, "test")
        assertFalse(VisualAssetPolicy45.canShip(by))
        assertTrue(VisualAssetPolicy45.requiresCredits(by))
    }

    @Test fun unknownOrRejectedArtCannotShip() {
        val rejected = VisualAssetSource("x", "x", "x", VisualAssetLicense.REJECT, false, false, "test")
        assertFalse(VisualAssetPolicy45.canShip(rejected))
    }
}
