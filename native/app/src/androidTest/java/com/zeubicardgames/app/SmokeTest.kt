package com.zeubicardgames.app
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertExists
import org.junit.Rule
import org.junit.Test
class SmokeTest { @get:Rule val rule = createAndroidComposeRule<MainActivity>(); @Test fun homeIsVisible() { rule.onNodeWithText("Booster du moment").assertExists() } }
