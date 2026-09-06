/*
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
package ru.pravbeseda.currencyedittext.watchers

import android.widget.EditText
import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import ru.pravbeseda.currencyedittext.util.emptyChar
import java.lang.ref.WeakReference
import java.text.DecimalFormatSymbols

private const val GROUP_SIZE = 3

class CurrencyInputWatcher(
    private val editTextRef: WeakReference<EditText>,
    private val config: CurrencyInputWatcherConfig,
) : EasyTextWatcher() {
    init {
        if (config.maxNumberOfDecimalPlaces < 0) {
            throw IllegalArgumentException(
                "Maximum number of Decimal Places must be a positive integer",
            )
        }
    }

    private val editText: EditText? get() = editTextRef.get()

    private val decimalSeparator: Char =
        if (config.decimalSeparator !== null) {
            config.decimalSeparator
        } else {
            DecimalFormatSymbols.getInstance(config.locale).decimalSeparator
        }
    private val groupingSeparator =
        if (config.groupingSeparator !== null) {
            config.groupingSeparator
        } else {
            DecimalFormatSymbols.getInstance(config.locale).groupingSeparator
        }

    override fun onTextModified(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?,
    ) {
        var resultText: String = newText ?: ""
        var sign = ""
        var position = editPosition ?: config.currencySymbol.length

        // Replace inserted comma or point to decimalSeparator
        if (arrayOf(
                ",",
                ".",
            ).contains(newPartOfText) && newPartOfText != decimalSeparator.toString()
        ) {
            resultText =
                resultText.replaceRange(position - 1, position, decimalSeparator.toString())
        }

        // Replace single zero to inserted digit
        if (oldText?.removePrefix(config.currencySymbol) == "0" && newPartOfText?.length == 1) {
            resultText = resultText.replaceFirst("0", "")
        }

        // Remove decimal zeros after removing decimal separator
        if (newPartOfText == "" &&
            oldText?.endsWith("${decimalSeparator}00") == true &&
            newText?.contains(decimalSeparator) != true
        ) {
            resultText = resultText.removeSuffix("00")
        }

        // Remove decimal separator from newPartOfText when maxNumberOfDecimalPlaces is 0
        if (config.maxNumberOfDecimalPlaces == 0 && arrayOf(",", ".").contains(newPartOfText)) {
            resultText = resultText.replaceRange(position - 1, position, "")
            position--
        }

        // Place sign minus before value
        val numberOfMinus = resultText.count { it == '-' }
        if (config.negativeValueAllow) {
            sign = if (numberOfMinus == 1) "-" else ""
            if (numberOfMinus > 1 && position > config.currencySymbol.length) {
                position -= 2
            }
        } else {
            if (position >= numberOfMinus) {
                position -= numberOfMinus
            }
        }
        resultText = resultText.replace("-", "")

        // Prevent manual removing currency symbol
        if (!resultText.startsWith(config.currencySymbol)) {
            resultText =
                config.currencySymbol +
                resultText.trimStart {
                    config.currencySymbol.toCharArray().contains(it)
                }
        }

        // Leave last decimalSeparator only
        val lastIndex = resultText.lastIndexOf(decimalSeparator)
        if (lastIndex > -1) {
            val firstPart = resultText.substring(0, lastIndex)
            val secondPart = resultText.substring(lastIndex + 1)
            val decimalSeparatorNumber = firstPart.count { it == decimalSeparator }
            resultText =
                firstPart.replace(decimalSeparator.toString(), "") + decimalSeparator + secondPart
            position -= decimalSeparatorNumber
            if (position < 0) position = 0
        }

        val newTextWithoutGroupingSeparators =
            resultText.replace("[^\\d$decimalSeparator]".toRegex(), "")

        // Calc decimal separator position (without grouping separators)
        val decimalSeparatorPos = newTextWithoutGroupingSeparators.indexOf(decimalSeparator)

        // Cursor position calculation (in text without separators)
        var cursorPosition =
            position.let {
                val end = if (it > resultText.length) resultText.length else it
                val text = resultText.substring(0, end)
                val curSpaceCount = countMatches(text, groupingSeparator.toString())
                val spaceCountInCurrencySymbol =
                    countMatches(config.currencySymbol, groupingSeparator.toString())
                it - curSpaceCount + spaceCountInCurrencySymbol
            }

        var integerPart =
            if (decimalSeparatorPos == -1) {
                newTextWithoutGroupingSeparators
            } else {
                newTextWithoutGroupingSeparators.substring(0, decimalSeparatorPos)
            }

        var fractionalPart =
            if (decimalSeparatorPos == -1) {
                ""
            } else {
                newTextWithoutGroupingSeparators.substring(decimalSeparatorPos + 1)
            }
        if (fractionalPart.length > config.maxNumberOfDecimalPlaces) {
            fractionalPart = fractionalPart.substring(0, config.maxNumberOfDecimalPlaces)
        }

        if (integerPart.isEmpty() && (newPartOfText == "0" || fractionalPart.isNotEmpty())) {
            integerPart = "0"
            cursorPosition++
        }

        resultText = integerPart
        if (decimalSeparatorPos > -1 && config.maxNumberOfDecimalPlaces > 0) {
            if (resultText == "") {
                resultText = "0"
                cursorPosition++
            }
            if (config.decimalZerosPadding && fractionalPart.isNotEmpty() &&
                fractionalPart.length < config.maxNumberOfDecimalPlaces
            ) {
                val zeros = "0".repeat(config.maxNumberOfDecimalPlaces - fractionalPart.length)
                fractionalPart += zeros
            }
            resultText += decimalSeparator + fractionalPart
        }

        setText(resultText, cursorPosition, config.currencySymbol, sign)
    }

    fun getDecimalSeparator(): Char = decimalSeparator

    fun getGroupingSeparator(): Char = groupingSeparator

    private fun countMatches(
        string: String?,
        pattern: String,
    ): Int {
        if (string.isNullOrEmpty()) {
            return 0
        }
        val res = string.split(pattern)
        return res.toTypedArray().size - 1
    }

    private fun setText(
        resultText: String,
        resultEditPosition: Int,
        currencySymbol: String,
        sign: String,
    ) {
        val formatted =
            calculateSpacing(
                resultText = resultText,
                resultEditPosition = resultEditPosition,
                currencySymbol,
                sign,
            )

        editText?.setText(formatted.text)
        editText?.setSelection(formatted.cursorPosition)

        config.onValueChanged?.invoke(formatted.text)
    }

    private fun calculateSpacing(
        resultText: String,
        resultEditPosition: Int,
        currencySymbol: String,
        sign: String,
    ): TextWithCursor {
        val dotPos = resultText.indexOf(decimalSeparator)

        val textBeforeDot =
            if (dotPos == -1) {
                resultText
            } else {
                resultText.substring(0, dotPos)
            }

        val textAfterDot =
            if (dotPos == -1) {
                ""
            } else {
                decimalSeparator + resultText.substring(dotPos + 1)
            }

        val grouped =
            insertGroupingSeparators(
                integerText = textBeforeDot,
                cursorPosition = resultEditPosition,
                prefixLength = currencySymbol.length + sign.length,
            )

        val result = currencySymbol + sign + grouped.text + textAfterDot

        var resultPosition = grouped.cursorPosition
        if (resultPosition < currencySymbol.length) resultPosition = currencySymbol.length
        if (resultPosition > result.length) resultPosition = result.length

        return TextWithCursor(result, resultPosition)
    }

    /**
     * Inserts a grouping separator every three digits of [integerText], moving the cursor
     * along with the digits it stands after.
     */
    private fun insertGroupingSeparators(
        integerText: String,
        cursorPosition: Int,
        prefixLength: Int,
    ): TextWithCursor {
        if (groupingSeparator == emptyChar) {
            return TextWithCursor(integerText, cursorPosition)
        }

        var text = integerText
        var position = cursorPosition
        var index = integerText.length
        repeat(integerText.length / GROUP_SIZE) {
            index -= GROUP_SIZE
            if (index > 0) {
                if (index < position - prefixLength) {
                    position++
                }
                text = StringBuilder(text).insert(index, groupingSeparator).toString()
            }
        }
        return TextWithCursor(text, position)
    }

    private data class TextWithCursor(
        val text: String,
        val cursorPosition: Int,
    )
}
