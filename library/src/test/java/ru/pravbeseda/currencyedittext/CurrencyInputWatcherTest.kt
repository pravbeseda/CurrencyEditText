/**
 * Copyright (c) 2022-2023 Alexander Ivanov
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

import android.text.Editable
import java.lang.IllegalArgumentException
import java.lang.ref.WeakReference
import org.junit.Assert
import org.junit.Test
import org.mockito.Mockito.*
import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import ru.pravbeseda.currencyedittext.model.LocaleVars
import ru.pravbeseda.currencyedittext.watchers.CurrencyInputWatcher

class CurrencyInputWatcherTest {

    private val locales = listOf(
        LocaleVars("en-NG", '.', ',', "$ "),
        LocaleVars("en-US", '.', ',', "$ "),
        LocaleVars("da-DK", ',', '.', "$ "),
        LocaleVars("fr-CA", '.', ' ', "$ "),
        LocaleVars("ru-Ru", ',', ' ', "")
    )

    @Test
    fun `Should keep currency symbol as hint when enabled and move cursor to front when edit text is set to empty string`() {
        for (locale in locales) {
            val currentEditTextContent = ""
            val expectedText = locale.currencySymbol
            val expectedCursorPosition = expectedText.length

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText, times(1)).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should set text to "$ 5" when text is set to "5"`() {
        for (locale in locales) {
            val currentEditTextContent = "5"
            val expectedText = "${locale.currencySymbol}5"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should set text to "$ 40" when text is set to "40"`() {
        for (locale in locales) {
            val currentEditTextContent = "40"
            val expectedText = "${locale.currencySymbol}40"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should set text to "$ 900" when text is set to "900"`() {
        for (locale in locales) {
            val currentEditTextContent = "900"
            val expectedText = "${locale.currencySymbol}900"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should set text to "$ 1,000" when text is set to "1000"`() {
        for (locale in locales) {
            val currentEditTextContent = "1000"
            val expectedText = "${locale.currencySymbol}1${locale.groupingSeparator}000"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should set text to "$ 15,420point50" when text is set to "15420point50"`() {
        for (locale in locales) {
            val currentEditTextContent = "15420${locale.decimalSeparator}50"
            val expectedText =
                "${locale.currencySymbol}15${locale.groupingSeparator}420${locale.decimalSeparator}50"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep the decimal symbol when the edit text does not contain a decimal symbol before and it is clicked`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}1${locale.groupingSeparator}000"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}000${locale.decimalSeparator}"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + locale.decimalSeparator)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should set text to "$ 10,002" when previous text is "$ 1,000" and "2" is clicked`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}1${locale.groupingSeparator}000"
            val expectedText = "${locale.currencySymbol}10${locale.groupingSeparator}002"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + "2")

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should automatically append a zero to the decimal separator when the edit text is empty and the decimal operator is clicked`() {
        for (locale in locales) {
            val currentEditTextContent = locale.currencySymbol
            val expectedText = "${locale.currencySymbol}0${locale.decimalSeparator}"
            val expectedCursorPosition =
                expectedText.length // cursor should be at the end of the text

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + locale.decimalSeparator)

            val start = currentEditTextContent.length
            watcher.beforeTextChanged(
                currentEditTextContent,
                start,
                1,
                start + 1
            )
            watcher.onTextChanged(editable, start, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should keep the single decimal digit when there are no decimal digits and a digit is added after the decimal symbol`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}5"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + "5")

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep two decimal digits when there is one decimal digit and a digit is added after the decimal symbol`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}5"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + "0")

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep the current text as is when there are two decimal digits and a digit is added after the decimal symbol`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"
            val expectedCursorPosition = expectedText.length

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + "9")

            watcher.beforeTextChanged(
                currentEditTextContent,
                currentEditTextContent.length,
                1,
                currentEditTextContent.length + 1
            )
            watcher.onTextChanged(editable, currentEditTextContent.length, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should keep the current text as is when there is two decimal digit and multiple digits are added after the decimal symbol`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + "92293948842")

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep only one decimal symbol when a decimal symbol is present and it is clicked again`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"
            val expectedText =
                "${locale.currencySymbol}132${locale.groupingSeparator}050${locale.decimalSeparator}"

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + locale.decimalSeparator)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should not allow a delete when edit text is set to currency symbol and a delete is clicked`() {
        for (locale in locales) {
            val currentEditTextContent = locale.currencySymbol
            val expectedText = locale.currencySymbol

            val (editText, editable, watcher) = setupTestVariables(locale)
            if (currentEditTextContent.isNotEmpty()) {
                `when`(editable.toString()).thenReturn(currentEditTextContent.removeFirstChar())
            } else {
                `when`(editable.toString()).thenReturn(currentEditTextContent)
            }

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should not allow a delete when edit text is set to currency symbol and a delete is clicked at the zeroth index`() {
        for (locale in locales) {
            val currentEditTextContent = locale.currencySymbol
            val expectedText = locale.currencySymbol

            val (editText, editable, watcher) = setupTestVariables(locale)
            if (currentEditTextContent.isNotEmpty()) {
                `when`(editable.toString()).thenReturn(currentEditTextContent.removeFirstChar())
            } else {
                `when`(editable.toString()).thenReturn(currentEditTextContent)
            }

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep a default of 2 decimal places when the max dp value isn't specified`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50992"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50"

            val (editText, editable) = setupTestVariables(locale)
            val config = CurrencyInputWatcherConfig(
                currencySymbol = locale.currencySymbol,
                locale = locale.tag.toLocale(),
                decimalSeparator = locale.decimalSeparator,
                groupingSeparator = locale.groupingSeparator,
                maxNumberOfDecimalPlaces = 2
            )
            val watcherWithDefaultDP = CurrencyInputWatcher(
                WeakReference(editText),
                config
            )
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcherWithDefaultDP.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should throw an Exception when maximum dp is bellow zero`() {
        for (locale in locales) {
            try {
                val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = -1)
                Assert.fail("Should have caught an illegalArgumentException at this point")
            } catch (e: IllegalArgumentException) {
            }
        }
    }

    @Test
    fun `Should keep only one decimal place when maximum dp is set to 1`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}50992"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}5"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 1)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep only two decimal places when maximum dp is set to 2`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}51992"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}51"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 2)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep only three decimal places when maximum dp is set to 3`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}51992"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}519"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 3)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep only seven decimal digits when maximum dp is set to 7`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}519923345634"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}5199233"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 7)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should keep up to ten decimal places when maximum dp is set to 10`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}519923345634"
            val expectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}5199233456"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 10)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should change maximum decimal digits to 3 if setMaxNumberOfDecimalPlaces(3) is called after being init with decimal digits 2`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}519923345634"
            val firstExpectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}51"
            val secondExpectedText =
                "${locale.currencySymbol}1${locale.groupingSeparator}320${locale.decimalSeparator}519"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 2)
            val secondWatcher = locale.toWatcher(
                editText,
                3,
                locale.decimalSeparator,
                locale.groupingSeparator,
                false
            )
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)
            secondWatcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(firstExpectedText)
            verify(editText, times(1)).setText(secondExpectedText)
        }
    }

    @Test
    fun `Should retain valid number if imputed`() {
        // This test tries to replicate issue #29 which fails on some devices and passes for some.
        // It however passes on my local. but might be helpful to have the test in here.
        for (locale in locales) {
            val currentEditTextContent = "515${locale.decimalSeparator}809"
            val expectedText = "${locale.currencySymbol}515${locale.decimalSeparator}809"

            val (editText, editable, watcher) = setupTestVariables(locale, decimalPlaces = 3)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `Should change cursor position after new input`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}1"
            val expectedText = "${locale.currencySymbol}12"
            val expectedCursorPosition = expectedText.length

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn(currentEditTextContent + "2")

            watcher.beforeTextChanged(
                currentEditTextContent,
                locale.currencySymbol.length + 1,
                1,
                locale.currencySymbol.length + 2
            )
            watcher.onTextChanged(editable, locale.currencySymbol.length + 1, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should not change sign by pressing minus if it's disallowed`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}100"
            val expectedText = "${locale.currencySymbol}100"
            val expectedCursorPosition = expectedText.length - 1

            val (editText, editable, watcher) = setupTestVariables(locale)
            `when`(editable.toString()).thenReturn("${locale.currencySymbol}10-0")

            watcher.beforeTextChanged(
                currentEditTextContent,
                currentEditTextContent.length,
                0,
                1
            )
            watcher.onTextChanged(editable, currentEditTextContent.length - 1, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should change sign by pressing minus if it's allowed`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}1"
            val expectedText = "${locale.currencySymbol}-1"
            val expectedCursorPosition = expectedText.length

            val (editText, editable, watcher) = setupTestVariables(locale, 2, true)
            `when`(editable.toString()).thenReturn("$currentEditTextContent-")

            watcher.beforeTextChanged(
                currentEditTextContent,
                currentEditTextContent.length,
                1,
                currentEditTextContent.length + 1
            )
            watcher.onTextChanged(editable, locale.currencySymbol.length + 1, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should remove sign by pressing minus and don't lost position`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}-12"
            val expectedText = "${locale.currencySymbol}12"
            val expectedCursorPosition = expectedText.length - 1

            val (editText, editable, watcher) = setupTestVariables(locale, 2, true)
            `when`(editable.toString()).thenReturn("${locale.currencySymbol}-1-2")

            val start = currentEditTextContent.length - 1
            watcher.beforeTextChanged(
                currentEditTextContent,
                start,
                1,
                currentEditTextContent.length
            )
            watcher.onTextChanged(editable, start, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `Should remove sign by pressing minus and don't lost position when number is big`() {
        for (locale in locales) {
            val currentEditTextContent = "${locale.currencySymbol}-12${locale.groupingSeparator}255"
            val expectedText = "${locale.currencySymbol}12${locale.groupingSeparator}255"
            val expectedCursorPosition = locale.currencySymbol.length + 1 // cursor after 1

            val (editText, editable, watcher) = setupTestVariables(locale, 0, true)
            `when`(editable.toString()).thenReturn(
                "${locale.currencySymbol}-1-2${locale.groupingSeparator}255"
            )

            val start = expectedCursorPosition + 1 // cursor after -1
            watcher.beforeTextChanged(
                currentEditTextContent,
                start,
                1,
                currentEditTextContent.length
            )
            watcher.onTextChanged(editable, start, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `should remove any non digit character`() {
        for (locale in locales) {
            val currentEditTextContent = "- 10006metres"
            val expectedText =
                "${locale.currencySymbol}-10${locale.groupingSeparator}006"

            val (editText, editable, watcher) = setupTestVariables(locale, 2, true)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `should remove decimal part if maxNumberOfDecimalPlaces = 0`() {
        for (locale in locales) {
            val currentEditTextContent = "100${locale.decimalSeparator}568"
            val expectedText = "${locale.currencySymbol}100"

            val (editText, editable, watcher) = setupTestVariables(locale, 0)
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `should prevent from entering decimal separator when maxNumberOfDecimalPlaces = 0`() {
        for (locale in locales) {
            val currentEditTextContent = "568"
            val expectedText = "${locale.currencySymbol}568"
            val expectedCursorPosition = locale.currencySymbol.length + 1

            val (editText, editable, watcher) = setupTestVariables(locale, 0)
            `when`(editable.toString()).thenReturn(
                "${locale.currencySymbol}5${locale.decimalSeparator}68"
            )
            watcher.beforeTextChanged(
                currentEditTextContent,
                expectedCursorPosition,
                1,
                locale.currencySymbol.length + 2
            )
            watcher.onTextChanged(editable, locale.currencySymbol.length + 1, 0, 1)
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `should move left cursor after decimal separator input`() {
        for (locale in locales) {
            val currentEditTextContent = "100"
            val currentCursorPosition = 2
            val expectedText = "${locale.currencySymbol}10${locale.decimalSeparator}0"
            val expectedCursorPosition = locale.currencySymbol.length + 3

            val (editText, editable, watcher) = setupTestVariables(locale, 2)
            `when`(editable.toString()).thenReturn(
                "${locale.currencySymbol}10${locale.decimalSeparator}0"
            )

            watcher.beforeTextChanged(
                currentEditTextContent,
                currentCursorPosition,
                1,
                expectedCursorPosition
            )
            watcher.onTextChanged(
                editable,
                locale.currencySymbol.length + currentCursorPosition,
                0,
                1
            )
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `should replace dot or comma with decimal separator and move left cursor`() {
        for (locale in locales) {
            val currentEditTextContent = "100"
            val currentCursorPosition = 2
            val expectedText = "${locale.currencySymbol}10${locale.decimalSeparator}0"
            val expectedCursorPosition = locale.currencySymbol.length + 3

            val (editText, editable, watcher) = setupTestVariables(locale, 2)
            val separator = if (locale.decimalSeparator == '.') ',' else '.' // opposite separator
            `when`(editable.toString()).thenReturn("${locale.currencySymbol}10${separator}0")

            watcher.beforeTextChanged(
                currentEditTextContent,
                currentCursorPosition,
                1,
                expectedCursorPosition
            )
            watcher.onTextChanged(
                editable,
                locale.currencySymbol.length + currentCursorPosition,
                0,
                1
            )
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(expectedText)
            verify(editText).setSelection(expectedCursorPosition)
        }
    }

    @Test
    fun `shouldn't move cursor after click on decimal separator before other decimal separator`() {
        for (locale in locales) {
            val currentEditTextContent =
                "${locale.currencySymbol}1${locale.groupingSeparator}000${locale.decimalSeparator}01"
            val currentCursorPosition = locale.currencySymbol.length + 3
            val (editText, editable, watcher) = setupTestVariables(locale, 2)
            `when`(editable.toString()).thenReturn(
                "${locale.currencySymbol}1${locale.groupingSeparator}0${locale.decimalSeparator}00${locale.decimalSeparator}01"
            )

            watcher.beforeTextChanged(
                currentEditTextContent,
                currentCursorPosition,
                1,
                currentCursorPosition
            )
            watcher.onTextChanged(
                editable,
                currentCursorPosition,
                0,
                1
            )
            watcher.afterTextChanged(editable)

            verify(editText, times(1)).setText(currentEditTextContent)
            verify(editText).setSelection(currentCursorPosition)
        }
    }

    @Test
    fun `should set text to "$ 900,4" when text is set to "900,4" and decimalZerosPadding if false`() {
        for (locale in locales) {
            val currentEditTextContent = "900${locale.decimalSeparator}4"
            val expectedText = "${locale.currencySymbol}900${locale.decimalSeparator}4"

            val (editText, editable, watcher) = setupTestVariables(
                locale = locale,
                decimalZerosPadding = false
            )
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    @Test
    fun `should set text to "$ 900,40" when text is set to "900,4" and decimalZerosPadding if true`() {
        for (locale in locales) {
            val currentEditTextContent = "900${locale.decimalSeparator}4"
            val expectedText = "${locale.currencySymbol}900${locale.decimalSeparator}40"

            val (editText, editable, watcher) = setupTestVariables(
                locale = locale,
                decimalZerosPadding = true
            )
            `when`(editable.toString()).thenReturn(currentEditTextContent)

            watcher.runAllWatcherMethods(editable)

            // Verify that the EditText's text was set to the expected text
            verify(editText, times(1)).setText(expectedText)
        }
    }

    private fun setupTestVariables(
        locale: LocaleVars,
        decimalPlaces: Int = 2,
        negativeValueAllow: Boolean = false,
        decimalZerosPadding: Boolean = false
    ): TestVars {
        val editText = mock(CurrencyEditText::class.java)
        val editable = mock(Editable::class.java)
        `when`(editText.text).thenReturn(editable)
        `when`(editable.append(isA(String::class.java))).thenReturn(editable)
        val watcher = locale.toWatcher(
            editText,
            decimalPlaces,
            locale.decimalSeparator,
            locale.groupingSeparator,
            negativeValueAllow,
            decimalZerosPadding
        )
        return TestVars(editText, editable, watcher)
    }
}

data class TestVars(
    val editText: CurrencyEditText,
    val editable: Editable,
    val watcher: CurrencyInputWatcher
)
