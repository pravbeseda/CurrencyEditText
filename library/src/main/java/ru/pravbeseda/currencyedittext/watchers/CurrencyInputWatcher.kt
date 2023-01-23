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
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
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
            throw IllegalArgumentException("Maximum number of Decimal Digits must be a positive integer")
        }
    }

    private val editText: EditText? get() = editTextRef.get()
    private val wholeNumberDecimalFormat =
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            applyPattern("#,##0")
            decimalFormatSymbols = getModifiedDecimalFormatSymbols()
        }

    val decimalFormatSymbols: DecimalFormatSymbols
        get() = wholeNumberDecimalFormat.decimalFormatSymbols

    private val decimalSeparator =
        if (paramDecimalSeparator !== null) paramDecimalSeparator else wholeNumberDecimalFormat.decimalFormatSymbols.decimalSeparator.toString()
    private val groupingSeparator =
        if (paramGroupingSeparator !== null) paramGroupingSeparator else wholeNumberDecimalFormat.decimalFormatSymbols.groupingSeparator.toString()

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
        if (negativeValueAllow) {
            val numberOfMinus = resultText.count { it == '-' }
            sign = if (numberOfMinus == 1) "-" else ""
            if (numberOfMinus > 1 && position > currencySymbol.length) {
                position -= 2
            }
        }
        resultText = resultText.replace("-", "")

        // Remove content after second decimalSeparator with itself
        val decimalParts = resultText.split(decimalSeparator)
        resultText = decimalParts[0]
        if (decimalParts.size > 1) resultText += decimalSeparator + decimalParts[1]

        // Prevent manual removing currency symbol
        if (!resultText.startsWith(currencySymbol)) {
            resultText =
                currencySymbol + resultText.trimStart { currencySymbol.toCharArray().contains(it) }
        }

        val newTextWithoutGroupingSeparators =
            resultText.replace(currencySymbol, "").replace(groupingSeparator, "")

        // Calc decimal separator position (without grouping separators)
        val decimalSeparatorPos = newTextWithoutGroupingSeparators.indexOf(decimalSeparator)

        // Cursor position calculation (in text without separators)
        val cursorPosition = position.let {
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
            }
            resultText += decimalSeparator + fractionalPart
        }

        setText(resultText, cursorPosition, currencySymbol, sign)
    }

    private fun getModifiedDecimalFormatSymbols(): DecimalFormatSymbols {
        val unusualSymbols = DecimalFormatSymbols()
        if (!paramDecimalSeparator.isNullOrEmpty()) {
            unusualSymbols.decimalSeparator = paramDecimalSeparator[0]
        }
        if (!paramGroupingSeparator.isNullOrEmpty()) {
            unusualSymbols.groupingSeparator = paramGroupingSeparator[0]
        }
        return unusualSymbols
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
