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
import java.util.*

class CurrencyInputWatcher(
    private val editTextRef: WeakReference<EditText>,
    private val currencySymbol: String,
    locale: Locale,
    private val paramDecimalSeparator: String? = null,
    private val paramGroupingSeparator: String? = null,
    private val maxNumberOfDecimalPlaces: Int = 2,
    private val negativeValueAllow: Boolean = false,
    private val onValueChanged: ((String?) -> Unit)? = null
) : EasyTextWatcher() {

    init {
        if (maxNumberOfDecimalPlaces < 1) {
            throw IllegalArgumentException(
                "Maximum number of Decimal Digits must be a positive integer"
            )
        }
    }

    private val editText: EditText? get() = editTextRef.get()

    private val decimalSeparator =
        if (paramDecimalSeparator !== null) {
            paramDecimalSeparator
        } else {
            DecimalFormatSymbols.getInstance(locale).decimalSeparator.toString()
        }
    private val groupingSeparator =
        if (paramGroupingSeparator !== null) {
            paramGroupingSeparator
        } else {
            DecimalFormatSymbols.getInstance(locale).groupingSeparator.toString()
        }

    override fun onTextModified(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?
    ) {
        var resultText: String = newText ?: ""
        var sign = ""
        var position = editPosition ?: currencySymbol.length

        // Replace inserted comma or point to decimalSeparator
        if (arrayOf(",", ".").contains(newPartOfText) && newPartOfText != decimalSeparator) {
            resultText = resultText.replaceRange(position - 1, position, decimalSeparator)
        }

        // Place sign minus before value
        val numberOfMinus = resultText.count { it == '-' }
        if (negativeValueAllow) {
            sign = if (numberOfMinus == 1) "-" else ""
            if (numberOfMinus > 1 && position > currencySymbol.length) {
                position -= 2
            }
        } else {
            position -= numberOfMinus
        }
        resultText = resultText.replace("-", "")

        // Prevent manual removing currency symbol
        if (!resultText.startsWith(currencySymbol)) {
            resultText =
                currencySymbol + resultText.trimStart { currencySymbol.toCharArray().contains(it) }
        }

        // Leave last decimalSeparator only
        val lastIndex = resultText.lastIndexOf(decimalSeparator)
        if (lastIndex > -1) {
            val firstPart = resultText.substring(0, lastIndex)
            val secondPart = resultText.substring(lastIndex + 1)
            resultText = firstPart.replace(decimalSeparator, "") + decimalSeparator + secondPart
        }

        val newTextWithoutGroupingSeparators =
            resultText.replace("[^\\d$decimalSeparator]".toRegex(), "")

        // Calc decimal separator position (without grouping separators)
        val decimalSeparatorPos = newTextWithoutGroupingSeparators.indexOf(decimalSeparator)

        // Cursor position calculation (in text without separators)
        var cursorPosition = position.let {
            val text = newText?.substring(0, it)
            val curSpaceCount = countMatches(text, groupingSeparator)
            val spaceCountInCurrencySymbol = countMatches(currencySymbol, groupingSeparator)
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
        if (fractionalPart.length > maxNumberOfDecimalPlaces) {
            fractionalPart = fractionalPart.substring(0, maxNumberOfDecimalPlaces)
        }

        resultText = integerPart
        if (decimalSeparatorPos > -1) {
            if (resultText == "") {
                resultText = "0"
                cursorPosition++
            }
            resultText += decimalSeparator + fractionalPart
        }

        setText(resultText, cursorPosition, currencySymbol, sign)
    }

    public fun getDecimalSeparator(): String {
        return decimalSeparator
    }

    fun getGroupingSeparator(): String {
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

        onValueChanged?.invoke(text.toString())
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
        for (i in 1 until spaceCount + 1) {
            index -= 3
            if (index > 0) {
                if (index < resultPosition - sign.length - currencySymbol.length) {
                    resultPosition++
                }
                val sb = StringBuilder()
                textBeforeDot = sb.append(textBeforeDot).insert(index, groupingSeparator).toString()
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
