package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.data.model.MemberProfile
import com.example.data.model.MemberRole
import com.example.ui.components.DigitalCredentialCard
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    val sampleMember = MemberProfile(
      id = 1,
      fullName = "Eduardo Martínez",
      nickname = "Rider TX",
      memberNumber = "TX-077",
      cedulaDni = "V-24.891.432",
      phone = "+58 412 555 8899",
      role = MemberRole.MIEMBRO_ACTIVO,
      chapterState = "Distrito Capital",
      bikeModel = "Keeway TX 200 SM",
      bikePlate = "AB9X88Z",
      bloodType = "O+",
      emergencyContactName = "María Martínez",
      emergencyContactPhone = "+58 414 123 4567"
    )

    composeTestRule.setContent {
      MyApplicationTheme {
        DigitalCredentialCard(member = sampleMember)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
