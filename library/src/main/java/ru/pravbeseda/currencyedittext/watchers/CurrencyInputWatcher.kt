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
package ru.pravbeseda.currencyedittext.watchers

import android.widget.EditText
import java.lang.ref.WeakReference
import java.text.DecimalFormatSymbols
import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import ru.pravbeseda.currencyedittext.util.emptyChar

class CurrencyInputWatcher(
    private val editTextRef: WeakReference<EditText>,
    private val config: CurrencyInputWatcherConfig
) : EasyTextWatcher() {

    init {
        if (config.maxNumberOfDecimalPlaces < 0) {
            throw IllegalArgumentException(
                "Maximum number of Decimal Places must be a positive integer"
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
        editPosition: Int?
    ) {
        var resultText: String = newText ?: ""
        var sign = ""
        var position = editPosition ?: config.currencySymbol.length

        // Replace inserted comma or point to decimalSeparator
        if (arrayOf(
                ",",
                "."
            ).contains(newPartOfText) && newPartOfText != decimalSeparator.toString()
        ) {
            resultText =
                resultText.replaceRange(position - 1, position, decimalSeparator.toString())
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
                config.currencySymbol + resultText.trimStart {
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
        var cursorPosition = position.let {
            val end = if (it > resultText.length) resultText.length else it
            val text = resultText.substring(0, end)
            val curSpaceCount = countMatches(text, groupingSeparator.toString())
            val spaceCountInCurrencySymbol =
                countMatches(config.currencySymbol, groupingSeparator.toString())
            it - curSpaceCount + spaceCountInCurrencySymbol
        }

        val integerPart = if (decimalSeparatorPos == -1) {
            newTextWithoutGroupingSeparators
        } else {
            newTextWithoutGroupingSeparators.substring(0, decimalSeparatorPos)
        }

        var fractionalPart = if (decimalSeparatorPos == -1) {
            ""
        } else {
            newTextWithoutGroupingSeparators.substring(decimalSeparatorPos + 1)
        }
        if (fractionalPart.length > config.maxNumberOfDecimalPlaces) {
            fractionalPart = fractionalPart.substring(0, config.maxNumberOfDecimalPlaces)
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

    fun getDecimalSeparator(): Char {
        return decimalSeparator
    }

    fun getGroupingSeparator(): Char {
        return groupingSeparator
    }

    private fun countMatches(string: String?, pattern: String): Int {
        if (string.isNullOrEmpty()) {
            return 0
        }
        val res = string.split(pattern)
        return res.toTypedArray().size - 1
    }

    private fun setText(
        resultText: String?,
        resultEditPosition: Int?,
        currencySymbol: String,
        sign: String
    ) {
        // Format text
        val (text, position) = calculateSpacing(
            resultText = resultText,
            resultEditPosition = resultEditPosition,
            currencySymbol,
            sign
        )

        editText?.setText((text as? String?) ?: "")

        // Set cursor
        editText?.setSelection((position as? Int?) ?: 0)

        config.onValueChanged?.invoke(text.toString())
    }

    private fun calculateSpacing(
        resultText: String?,
        resultEditPosition: Int?,
        currencySymbol: String,
        sign: String
    ): Array<Any?> {
        var resultPosition = resultEditPosition ?: currencySymbol.length
        val dotPos = resultText?.indexOf(decimalSeparator) ?: -1

        var textBeforeDot = if (dotPos == -1) {
            resultText ?: ""
        } else {
            resultText?.substring(0, dotPos) ?: ""
        }

        var textAfterDot = if (dotPos == -1) {
            null
        } else {
            resultText?.substring(dotPos + 1, resultText.length) ?: ""
        }

        val spaceCount = textBeforeDot.length / 3

        var index = textBeforeDot.length

        // Count all group separators and calc cursor position
        if (groupingSeparator != emptyChar) {
            for (i in 1 until spaceCount + 1) {
                index -= 3
                if (index > 0) {
                    if (index < resultPosition - sign.length - currencySymbol.length) {
                        resultPosition++
                    }
                    val sb = StringBuilder()
                    textBeforeDot =
                        sb.append(textBeforeDot).insert(index, groupingSeparator).toString()
                }
            }
        }

        textAfterDot = if (textAfterDot != null) {
            "$decimalSeparator$textAfterDot"
        } else {
            ""
        }

        // Final result
        val result = currencySymbol + sign + textBeforeDot + textAfterDot

        if (resultPosition < currencySymbol.length) resultPosition = currencySymbol.length
        if (resultPosition > result.length) resultPosition = result.length

        return arrayOf(result, resultPosition)
    }
}
