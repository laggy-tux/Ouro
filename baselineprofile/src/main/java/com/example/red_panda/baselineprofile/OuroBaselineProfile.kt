package com.example.red_panda.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.uiautomator.textAsString
import androidx.test.uiautomator.uiAutomator
import org.junit.Rule
import org.junit.Test

class OuroBaselineProfile {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun galleryTabNavigation() {
        baselineProfileRule.collect(packageName = TARGET_PACKAGE) {
            uiAutomator {
                startApp(TARGET_PACKAGE)
                repeat(3) {
                    onElement { textAsString() == "Albums" }.click()
                    onElement { textAsString() == "Home" }.click()
                }
            }
        }
    }

    private companion object {
        const val TARGET_PACKAGE = "com.yahshua.ouro"
    }
}
