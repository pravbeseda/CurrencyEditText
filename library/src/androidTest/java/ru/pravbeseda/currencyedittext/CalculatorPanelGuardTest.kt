/*
 * Copyright (c) Alexander Ivanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.pravbeseda.currencyedittext

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import androidx.test.core.app.launchActivity
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import ru.pravbeseda.currencyedittext.calculator.CalculatorPopup

/**
 * A field opens one panel at a time. Placement waits for the keyboard to go, so between the tap and
 * the panel appearing the button that opened it still takes taps: without a guard, the second one
 * opens a second panel.
 */
class CalculatorPanelGuardTest {
    @Test
    fun aSecondRequestWhileThePanelIsUpOpensNoSecondPanel() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var field: CurrencyEditText
            var first: CalculatorPopup? = null
            scenario.onActivity { activity ->
                field = activity.addField()
                field.showCalculator(field)
                first = field.calculatorPanel
                field.showCalculator(field)
            }

            assertSame("a second request must not open a second panel", first, panelOf(field))
            assertTrue("the panel is still the one on screen", isActive(field))
        }
    }

    @Test
    fun aPanelThatWasDismissedOpensAgain() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var field: CurrencyEditText
            scenario.onActivity { activity ->
                field = activity.addField()
                field.showCalculator(field)
            }
            val first = panelOf(field)

            closeTheOpenPanel(field)
            scenario.onActivity { field.showCalculator(field) }

            assertNotSame("the field opens a fresh panel", first, panelOf(field))
            assertTrue(isActive(field))
        }
    }

    @Test
    fun twoFieldsOnOneScreenEachOpenTheirOwnPanel() {
        launchActivity<TestActivity>().use { scenario ->
            lateinit var one: CurrencyEditText
            lateinit var two: CurrencyEditText
            scenario.onActivity { activity ->
                one = activity.addField()
                two = activity.addField()
                one.showCalculator(one)
                two.showCalculator(two)
            }

            assertNotSame("a panel of one field must not block the other", panelOf(one), panelOf(two))
            assertTrue(isActive(one))
            assertTrue(isActive(two))
        }
    }

    /**
     * Back closes the panel, which is how a user leaves it without a result. The panel's window takes
     * the focus a moment after it is shown, and a back sent before that would go to the activity, so
     * wait for the field's own window to lose the focus first.
     */
    private fun closeTheOpenPanel(field: CurrencyEditText) {
        waitFor("the panel never took the focus") { !onMainThread { field.hasWindowFocus() } }
        InstrumentationRegistry
            .getInstrumentation()
            .uiAutomation
            .performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK)
        waitFor("the panel is still up after back") { !isActive(field) }
    }

    private fun waitFor(
        complaint: String,
        settled: () -> Boolean,
    ) {
        val giveUpAt = SystemClock.uptimeMillis() + TIMEOUT_MS
        while (!settled()) {
            if (SystemClock.uptimeMillis() > giveUpAt) fail(complaint)
            SystemClock.sleep(POLL_MS)
        }
    }

    private fun isActive(field: CurrencyEditText): Boolean = onMainThread { field.calculatorPanel?.isActive == true }

    private fun panelOf(field: CurrencyEditText): CalculatorPopup? = onMainThread { field.calculatorPanel }

    // The panel and its field belong to the main thread, and so does every answer they give.
    private fun <T> onMainThread(read: () -> T): T {
        val answers = mutableListOf<T>()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { answers += read() }
        return answers.single()
    }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val POLL_MS = 50L
    }
}
