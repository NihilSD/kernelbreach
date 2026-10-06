package com.kernelbreach.app.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import com.kernelbreach.core.design.components.DotGridBackground
import com.kernelbreach.core.design.components.FeedbackBanner
import com.kernelbreach.core.design.components.HighlightPhrase
import com.kernelbreach.core.design.components.KbCard
import com.kernelbreach.core.design.components.Pill
import com.kernelbreach.core.design.components.PrimaryButton
import com.kernelbreach.core.design.components.SegmentBar
import com.kernelbreach.core.design.theme.KbTheme
import com.kernelbreach.core.design.theme.KernelBreachTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests for the Blueprint design-system components, in light and dark.
 *
 * Requires an Android SDK + Robolectric (so it runs on the JVM, no device). Record
 * goldens with `./gradlew :app:recordRoborazziDebug`, verify with
 * `:app:verifyRoborazziDebug`. See README "Known gaps" for extending this to full
 * per-screen goldens against design/screens/*.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w390dp-h844dp-xhdpi")
class BlueprintComponentsScreenshotTest {

    @get:Rule val composeRule = createComposeRule()

    @Test
    fun components_light() = capture(dark = false, name = "blueprint_components_light")

    @Test
    fun components_dark() = capture(dark = true, name = "blueprint_components_dark")

    private fun capture(dark: Boolean, name: String) {
        composeRule.setContent {
            KernelBreachTheme(forceDark = dark) {
                DotGridBackground(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text("Design language", style = KbTheme.type.headline, color = KbTheme.colors.ink)
                        HighlightPhrase("the key idea is highlighted")
                        SegmentBar(total = 6, completed = 3, current = 3)
                        KbCard { Text("A card on graph paper.", style = KbTheme.type.body, color = KbTheme.colors.ink) }
                        FeedbackBanner(correct = true)
                        FeedbackBanner(correct = false)
                        Pill("You are here")
                        PrimaryButton("Continue", onClick = {})
                    }
                }
            }
        }
        composeRule.onRoot().captureRoboImage("build/outputs/roborazzi/$name.png")
    }
}
