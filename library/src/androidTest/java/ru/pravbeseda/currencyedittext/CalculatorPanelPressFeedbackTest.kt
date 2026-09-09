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

import android.widget.Button
import androidx.test.core.app.launchActivity
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * The equals key is filled from `calculatorEqualsBackgroundColor`, and its borderless ripple moves
 * to the foreground so the fill does not replace it. `View.setBackground` clears the callback of
 * the background it replaces, so a ripple moved before the fill is set keeps its place and loses
 * the view it draws into: it never invalidates again and the key stops answering a press.
 */
class CalculatorPanelPressFeedbackTest {
    @Test
    fun theEqualsKeyStillDrawsItsRippleOnceItIsFilled() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val field = activity.addField()
                field.showCalculator(field)

                val equals =
                    field.calculatorPanel
                        ?.window
                        ?.contentView
                        ?.findViewById<Button>(R.id.currency_calculator_key_equals)
                assertNotNull("the panel is up, so the equals key is on it", equals)
                assertNotNull("the ripple moved to the foreground", equals?.foreground)
                assertSame(
                    "a ripple whose callback is gone never draws another frame",
                    equals,
                    equals?.foreground?.callback,
                )
            }
        }
    }
}
