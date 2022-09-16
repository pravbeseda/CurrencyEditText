/**
 * Copyright (c) 2022 Alexander Ivanov
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

import android.annotation.SuppressLint
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.*
import kotlin.math.min

class CurrencyInputWatcher(
    private val editText: EditText,
    private val currencySymbol: String,
    locale: Locale,
    private val decimalSeparator: String? = null,
    private val groupingSeparator: String? = null,
    private val maxNumberOfDecimalPlaces: Int = 2,
    private val negativeValueAllow: Boolean = false
) : EasyTextWatcher() {

    init {
        if (maxNumberOfDecimalPlaces < 1) {
            throw IllegalArgumentException("Maximum number of Decimal Digits must be a positive integer")
        }
    }

    companion object {
        const val FRACTION_FORMAT_PATTERN_PREFIX = "#,##0."
    }

    private var hasDecimalPoint = false
    private val wholeNumberDecimalFormat =
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            applyPattern("#,##0")
            decimalFormatSymbols = getModifiedDecimalFormatSymbols()
        }

    private val fractionDecimalFormat = (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
        decimalFormatSymbols = getModifiedDecimalFormatSymbols()
    }

    val decimalFormatSymbols: DecimalFormatSymbols
        get() = wholeNumberDecimalFormat.decimalFormatSymbols

    override fun onTextModified(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?
    ) {
        val currentDecimalSeparator = wholeNumberDecimalFormat.decimalFormatSymbols.decimalSeparator

        Log.d("!!!", "currencySymbol: '$currencySymbol', newPartOfText: '$newPartOfText', newText: '$newText', oldText: '$oldText', editPosition: $editPosition")
        var resultText: String = newText ?: ""

        // Place sign minus before value
        val sign = if (negativeValueAllow && resultText.count { it == '-' } == 1) "-" else ""
        resultText = resultText.replace("-", "");

        // Replace new "," or "." to decimalSeparator
        if (arrayOf(",", ".").contains(newPartOfText) && newPartOfText != currentDecimalSeparator.toString()) {
            val pos = editPosition ?: 0;
            resultText = resultText.replaceRange(pos-1, pos, currentDecimalSeparator.toString())
        }

        // Prevent manual removing currency symbol
        if (!resultText.startsWith(currencySymbol)) {
            resultText = currencySymbol + resultText.trimStart { currencySymbol.toCharArray().contains(it) }
        }

        // Remove currency symbol
        resultText = resultText.removePrefix(currencySymbol)

        val hasDecimalPoint = resultText.contains(currentDecimalSeparator)

        // Only first decimalSeparator is valid
        if (hasDecimalPoint) {
            val decimalParts = resultText.split(currentDecimalSeparator) as MutableList<String>
            resultText = decimalParts[0] + currentDecimalSeparator
            decimalParts.removeFirst()
            var decimalPart = decimalParts.joinToString("")
            if (decimalPart.length > maxNumberOfDecimalPlaces) {
                decimalPart = decimalPart.substring(0, maxNumberOfDecimalPlaces)
            }
            // "." => "0."
            if (resultText == currentDecimalSeparator.toString()) {
                resultText = "0$currentDecimalSeparator"
            }
            resultText += decimalPart
        }

        // val posDecimalSeparator = resultText.indexOfFirst{ it == currentDecimalSeparator }

        resultText = currencySymbol + sign + resultText
        editText.setText(resultText)
    }

    /*fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
        fractionDecimalFormat.isDecimalSeparatorAlwaysShown = true
    }

    fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
        hasDecimalPoint = s.toString().contains(decimalFormatSymbols.decimalSeparator.toString())
    }

    @SuppressLint("SetTextI18n")
    fun afterTextChanged(s: Editable) {
        var newInputString: String = s.toString()
        val isParsableString = try {
            fractionDecimalFormat.parse(newInputString)!!
            true
        } catch (e: ParseException) {
            false
        }

        if (newInputString.length < currencySymbol.length && !isParsableString) {
            editText.setText(currencySymbol)
            editText.setSelection(currencySymbol.length)
            return
        }

        if (newInputString == currencySymbol) {
            editText.setSelection(currencySymbol.length)
            return
        }

        editText.removeTextChangedListener(this)
        val startLength = editText.text.length
        try {
            var numberWithoutGroupingSeparator =
                parseMoneyValue(
                    newInputString,
                    decimalFormatSymbols.groupingSeparator.toString(),
                    currencySymbol
                )
            if (numberWithoutGroupingSeparator == decimalFormatSymbols.decimalSeparator.toString()) {
                numberWithoutGroupingSeparator = "0$numberWithoutGroupingSeparator"
            }

            numberWithoutGroupingSeparator = truncateNumberToMaxDecimalDigits(
                numberWithoutGroupingSeparator,
                maxNumberOfDecimalPlaces,
                decimalFormatSymbols.decimalSeparator
            )

            val parsedNumber = fractionDecimalFormat.parse(numberWithoutGroupingSeparator)!!
            val selectionStartIndex = editText.selectionStart
            if (hasDecimalPoint) {
                fractionDecimalFormat.applyPattern(
                    FRACTION_FORMAT_PATTERN_PREFIX +
                            getFormatSequenceAfterDecimalSeparator(numberWithoutGroupingSeparator)
                )
                val newText = "$currencySymbol${fractionDecimalFormat.format(parsedNumber)}"
                editText.setText(newText)
            } else {
                val newText = "$currencySymbol${wholeNumberDecimalFormat.format(parsedNumber)}"
                editText.setText(newText)
            }
            val endLength = editText.text.length
            val selection = selectionStartIndex + (endLength - startLength)
            if (selection > 0 && selection <= editText.text.length) {
                editText.setSelection(selection)
            } else {
                editText.setSelection(editText.text.length - 1)
            }
        } catch (e: ParseException) {
            e.printStackTrace()
        }
        editText.addTextChangedListener(this)
    }*/

    /**
     * @param number the original number to format
     * @return the appropriate zero sequence for the input number. e.g 156.1 returns "0",
     *  14.98 returns "00"
     */
    private fun getFormatSequenceAfterDecimalSeparator(number: String): String {
        val noOfCharactersAfterDecimalPoint = number.length - number.indexOf(decimalFormatSymbols.decimalSeparator) - 1
        return "0".repeat(min(noOfCharactersAfterDecimalPoint, maxNumberOfDecimalPlaces))
    }

    private fun getModifiedDecimalFormatSymbols(): DecimalFormatSymbols {
        val unusualSymbols = DecimalFormatSymbols()
        if (!decimalSeparator.isNullOrEmpty()) {
            unusualSymbols.decimalSeparator = decimalSeparator[0]
        }
        if (!groupingSeparator.isNullOrEmpty()) {
            unusualSymbols.groupingSeparator = groupingSeparator[0]
        }
        return unusualSymbols
    }
}
