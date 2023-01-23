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

import android.widget.EditText
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.*
import kotlin.math.min

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

    private val decimalSeparator = if (paramDecimalSeparator !== null) paramDecimalSeparator else wholeNumberDecimalFormat.decimalFormatSymbols.decimalSeparator.toString()
    private val groupingSeparator = if (paramGroupingSeparator !== null) paramGroupingSeparator else wholeNumberDecimalFormat.decimalFormatSymbols.groupingSeparator.toString()

    override fun onTextModified(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?
    ) {

        val gropingSeparatorsCount = countMatches(newText, groupingSeparator)
        var resultText: String = newText ?: ""
        var sign = ""
        var position = editPosition ?: currencySymbol.length

        // Replace inserted comma or point to decimalSeparator
        if (arrayOf(",", ".").contains(newPartOfText) && newPartOfText != decimalSeparator) {
            resultText = resultText.replaceRange(position-1, position, decimalSeparator)
        }

        // Place sign minus before value
        if (negativeValueAllow) {
            val numberOfMinus = resultText.count { it == '-' }
            sign = if (numberOfMinus == 1) "-" else ""
            if (numberOfMinus > 1 && position > currencySymbol.length) {
                position -=2
            }
        }
        resultText = resultText.replace("-", "")

        // Remove content after second decimalSeparator with itself
        val decimalParts = resultText.split(decimalSeparator)
        resultText = decimalParts[0]
        if (decimalParts.size > 1) resultText += decimalSeparator + decimalParts[1]

        // Prevent manual removing currency symbol
        if (!resultText.startsWith(currencySymbol)) {
            resultText = currencySymbol + resultText.trimStart { currencySymbol.toCharArray().contains(it) }
        }

        val newTextWithoutGroupingSeparators = resultText.replace(currencySymbol, "").replace(groupingSeparator, "") ?: ""

        // Calc decimal separator position (without grouping separators)
        val decimalSeparatorPos = newTextWithoutGroupingSeparators.indexOf(decimalSeparator)

        // Расчет позиции курсора (в тексте без разделителей)
        var cursorPosition = position.let {
            val text = newText?.substring(0, it)
            val curSpaceCount = countMatches(text, groupingSeparator, false)
            val spaceCountInCurrencySymbol = countMatches(currencySymbol, groupingSeparator, false)
            it - curSpaceCount + spaceCountInCurrencySymbol
        } ?: currencySymbol.length

        val newPartOfTextWithoutGroupingSeparators = newPartOfText?.replace(groupingSeparator, "") ?: ""
        val oldTextWithoutGroupingSeparators = oldText?.replace(groupingSeparator, "") ?: ""

        var integerPart = if (decimalSeparatorPos == -1) {
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

        // Если поле ввода состоит только из 0 - заменяем его введенным символом
//        if (newPartOfTextWithoutGroupingSeparators.isNotEmpty() && oldTextWithoutGroupingSeparators == "0" && fractionalPart == null && decimalSeparatorPos == -1 && cursorPosition == 1) {
//            setText(resultText = newPartOfTextWithoutGroupingSeparators, resultEditPosition = cursorPosition, currencySymbol, sign)
//            return
//        }


        resultText = integerPart
        if (decimalSeparatorPos > -1) {
            if (resultText == "") {
                resultText = "0"
            }
            resultText += decimalSeparator + fractionalPart
        }

        // val hasDecimalPoint = resultText.contains(decimalSeparator)

//        if (fractionalPart.isNotEmpty()) {
////            val decimalParts = resultText.split(decimalSeparator) as MutableList<String>
////            resultText = decimalParts[0] + decimalSeparator
////            decimalParts.removeFirst()
////            var decimalPart = decimalParts.joinToString("")
//            if (fractionalPart.length > maxNumberOfDecimalPlaces) {
//                fractionalPart = fractionalPart.substring(0, maxNumberOfDecimalPlaces)
//            }
//            // "." => "0."
//            if (resultText == decimalSeparator) {
//                resultText = "0$decimalSeparator"
//            }
//            resultText += fractionalPart
//        }

//        var numberWithoutGroupingSeparator =
//            parseMoneyValue(
//                newInputString,
//                decimalFormatSymbols.groupingSeparator.toString(),
//                currencySymbol
//            )

        // val posDecimalSeparator = resultText.indexOfFirst{ it == currentDecimalSeparator }

        setText(resultText, cursorPosition, currencySymbol, sign)
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
        if (!paramDecimalSeparator.isNullOrEmpty()) {
            unusualSymbols.decimalSeparator = paramDecimalSeparator[0]
        }
        if (!paramGroupingSeparator.isNullOrEmpty()) {
            unusualSymbols.groupingSeparator = paramGroupingSeparator[0]
        }
        return unusualSymbols
    }

    private fun countMatches(string: String?, pattern: String, trim: Boolean = true): Int {
        if (string.isNullOrEmpty()) {
            return 0
        }
        var res = string.split(pattern)
        if (trim) {
            res = res.dropLastWhile { it.isEmpty() }
        }
        return res.toTypedArray().size - 1
    }

    private fun setText(resultText: String?, resultEditPosition: Int?,  currencySymbol: String, sign: String) {
        // Устанавливаем разделители в текст
        val (text, position) = calculateSpacing(
            resultText = resultText,
            resultEditPosition = resultEditPosition,
            currencySymbol,
            sign
        )

        // Устанавливаем текст
        editText?.setText((text as? String?) ?: "")

        // Устанавливаем курсор на нужную позицию
        editText?.setSelection((position as? Int?) ?: 0)

        // Возвращаем колбэк
        onValueChanged?.invoke(text.toString())
    }

    private fun calculateSpacing(resultText: String?, resultEditPosition: Int?,  currencySymbol: String, sign: String): Array<Any?> {
        // Переменная для расчета позиции
        var resultPosition = resultEditPosition ?: currencySymbol.length

        // Расчет индекса точки в тексте
        val dotPos = resultText?.indexOf(decimalSeparator) ?: -1

        // Расчет текста до точки
        var textBeforeDot = if (dotPos == -1) {
            resultText ?: ""
        } else {
            resultText?.substring(0, dotPos) ?: ""
        }

        // Расчет текста после точки
        var textAfterDot = if (dotPos == -1) {
            null
        } else {
            resultText?.substring(dotPos + 1, resultText.length)?: ""
        }

        // Расчет количества отступов
        val spaceCount = textBeforeDot.length / 3

        // Индекс последнего символа
        var index = textBeforeDot.length

        // Перебираем все пробелы и устанавливаем их в результирующий текст
        // паралельно сдвигая курсор
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

        // Окончательный расчет текста после точки
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
