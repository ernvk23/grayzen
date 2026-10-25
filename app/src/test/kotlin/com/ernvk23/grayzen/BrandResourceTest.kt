package io.github.ernvk23.grayzen

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class BrandResourceTest {
    @Test fun launcherAndThemedVariantsShareTheGrayzenMark() {
        val legacy = source("src/main/res/mipmap-anydpi/ic_launcher.xml")
        val adaptive = source("src/main/res/mipmap-anydpi-v26/ic_launcher.xml")
        val themed = source("src/main/res/mipmap-anydpi-v33/ic_launcher.xml")
        val notification = source("src/main/kotlin/com/ernvk23/grayzen/SnoozeNotificationManager.kt")

        listOf(legacy, adaptive, themed).forEach {
            assertTrue(it.contains("@color/grayzen_launcher_background"))
            assertTrue(it.contains("@drawable/ic_grayzen_mark"))
        }
        assertTrue(themed.contains("<monochrome android:drawable=\"@drawable/ic_grayzen_mark\""))
        assertTrue(notification.contains("setSmallIcon(R.drawable.ic_grayzen_notification)"))
        assertTrue(source("src/main/res/values/colors.xml").contains("#12100E"))
        val drawables = Files.list(locate("src/main/res/drawable")).use { paths ->
            paths.map { it.fileName.toString() }.toList()
        }
        assertTrue(drawables.all { it.startsWith("ic_grayzen_") || it.startsWith("ic_tabler_") })
    }

    @Test fun grayzenMarkIsCenteredAndInsideTheAdaptiveSafeZone() {
        val mark = source("src/main/res/drawable/ic_grayzen_mark.xml")
        assertTrue(mark.contains("android:viewportWidth=\"108\""))
        assertTrue(mark.contains("android:viewportHeight=\"108\""))
        assertTrue(mark.contains("#FFF5EFE7"))
        assertTrue(mark.contains("#FFE3A85B"))
        assertFalse(mark.contains("strokeColor"))
        assertTrue(mark.contains("M51,24.15"))
        assertTrue(mark.contains("M57,24.15"))
        assertTrue(mark.contains("83.85"))

        val notification = source("src/main/res/drawable/ic_grayzen_notification.xml")
        assertTrue(notification.contains("android:width=\"24dp\""))
        assertTrue(notification.contains("android:height=\"24dp\""))
        assertTrue(notification.contains("#FFFFFFFF"))
    }

    @Test fun customFontsAndTheirLicensesAreRemoved() {
        val main = locate("src/main")
        val files = Files.walk(main).use { paths ->
            paths.filter(Files::isRegularFile).toList()
        }
        assertFalse(files.any { it.fileName.toString().contains("atkinson", ignoreCase = true) })
        assertFalse(files.any { it.fileName.toString().contains("chat_favour", ignoreCase = true) })

        val theme = source("src/main/kotlin/com/ernvk23/grayzen/ui/Theme.kt")
        assertTrue(theme.contains("FontFamily.SansSerif"))
        assertFalse(theme.contains("Atkinson"))
        assertFalse(theme.contains("ChatFavour"))

        val notices = Files.readString(locate("THIRD_PARTY_NOTICES.md"))
        assertTrue(notices.contains("Tabler"))
        assertFalse(notices.contains("Atkinson", ignoreCase = true))
        assertFalse(notices.contains("Chat Favour", ignoreCase = true))
    }

    private fun source(relative: String): String = Files.readString(locate(relative))

    private fun locate(relative: String): Path = candidates(relative)
        .firstOrNull(Files::exists) ?: error("Could not locate $relative")

    private fun candidates(relative: String): List<Path> = listOf(
        Path.of(relative),
        Path.of("app").resolve(relative),
        Path.of("..").resolve(relative),
        Path.of("..").resolve("app").resolve(relative),
    )
}
