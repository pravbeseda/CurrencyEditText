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
package ru.pravbeseda.currencyedittext.watchers

import android.widget.EditText
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.Mockito.mock
import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import java.lang.ref.WeakReference
import java.util.Locale

/**
 * The separators the field ends up using, once the locale has filled in whatever the configuration
 * left out. Two equal separators leave a number unreadable — `1,234,56` reads as 123456 — so one of
 * them has to move, and which one is what these tests pin down.
 */
class CurrencyInputWatcherSeparatorsTest {
    @Test
    fun `Issue #58 — a decimal separator equal to the locale's grouping one moves the grouping one`() {
        val watcher = watcher(decimalSeparator = ',', groupingSeparator = null)

        assertEquals(',', watcher.getDecimalSeparator())
        assertEquals('.', watcher.getGroupingSeparator())
    }

    @Test
    fun `Issue #58 — a grouping separator equal to the locale's decimal one moves the decimal one`() {
        val watcher = watcher(decimalSeparator = null, groupingSeparator = '.')

        assertEquals(',', watcher.getDecimalSeparator())
        assertEquals('.', watcher.getGroupingSeparator())
    }

    @Test
    fun `Two separators asked for and equal move the decimal one, as setSeparators has always done`() {
        val watcher = watcher(decimalSeparator = '.', groupingSeparator = '.')

        assertEquals(',', watcher.getDecimalSeparator())
        assertEquals('.', watcher.getGroupingSeparator())
    }

    @Test
    fun `Separators that do not collide are left alone`() {
        val watcher = watcher(decimalSeparator = null, groupingSeparator = null)

        assertEquals('.', watcher.getDecimalSeparator())
        assertEquals(',', watcher.getGroupingSeparator())
    }

    /** English, whose grouping separator is the comma and whose decimal separator is the point. */
    private fun watcher(
        decimalSeparator: Char?,
        groupingSeparator: Char?,
    ): CurrencyInputWatcher =
        CurrencyInputWatcher(
            WeakReference(mock(EditText::class.java)),
            CurrencyInputWatcherConfig(
                locale = Locale.forLanguageTag("en"),
                currencySymbol = "",
                decimalSeparator = decimalSeparator,
                groupingSeparator = groupingSeparator,
                maxNumberOfDecimalPlaces = 2,
                negativeValueAllow = false,
                decimalZerosPadding = false,
            ),
        )
}
