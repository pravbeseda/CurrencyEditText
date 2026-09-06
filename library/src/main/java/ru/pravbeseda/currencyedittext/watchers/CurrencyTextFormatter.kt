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

import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import ru.pravbeseda.currencyedittext.util.emptyChar

private const val GROUP_SIZE = 3

/** Text and the cursor position that belongs to it. */
internal data class TextWithCursor(
    val text: String,
    val cursorPosition: Int,
)

/**
 * Turns an edit of the field into the text the field should show and the position the cursor
 * should end up at. Holds no reference to the view: [CurrencyInputWatcher] does the talking.
 */
internal class CurrencyTextFormatter(
    private val config: CurrencyInputWatcherConfig,
    private val decimalSeparator: Char,
    private val groupingSeparator: Char,
) {
    fun format(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?,
    ): TextWithCursor {
        val normalized = normalizeInput(newPartOfText, newText, oldText, editPosition)
        val signed = extractSign(normalized)
        val withPrefix = restoreCurrencyPrefix(signed.value)
        val singleSeparator = keepLastDecimalSeparator(withPrefix)
        val number = buildNumber(singleSeparator, newPartOfText)

        return composeResult(number, signed.sign)
    }

    /** Applies the fixes that depend on what the user has just typed or deleted. */
    private fun normalizeInput(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?,
    ): TextWithCursor {
        var text = newText.orEmpty()
        var position = editPosition ?: config.currencySymbol.length
        val separatorTyped = newPartOfText == "," || newPartOfText == "."

        // Replace inserted comma or point to decimalSeparator
        if (separatorTyped && newPartOfText != decimalSeparator.toString()) {
            text = text.replaceRange(position - 1, position, decimalSeparator.toString())
        }

        // Replace single zero to inserted digit
        if (oldText?.removePrefix(config.currencySymbol) == "0" && newPartOfText?.length == 1) {
            text = text.replaceFirst("0", "")
        }

        // Remove decimal zeros after removing decimal separator
        if (newPartOfText == "" &&
            oldText?.endsWith("${decimalSeparator}00") == true &&
            newText?.contains(decimalSeparator) != true
        ) {
            text = text.removeSuffix("00")
        }

        // Remove the typed decimal separator when no decimal places are allowed
        if (config.maxNumberOfDecimalPlaces == 0 && separatorTyped) {
            text = text.replaceRange(position - 1, position, "")
            position--
        }

        return TextWithCursor(text, position)
    }

    /** Takes every minus out of the text and turns them into a single leading sign. */
    private fun extractSign(input: TextWithCursor): SignedText {
        val numberOfMinus = input.text.count { it == '-' }
        var position = input.cursorPosition
        var sign = ""

        if (config.negativeValueAllow) {
            sign = if (numberOfMinus == 1) "-" else ""
            if (numberOfMinus > 1 && position > config.currencySymbol.length) {
                position -= 2
            }
        } else if (position >= numberOfMinus) {
            position -= numberOfMinus
        }

        return SignedText(sign, TextWithCursor(input.text.replace("-", ""), position))
    }

    /** Puts the currency symbol back when the user has deleted part of it. */
    private fun restoreCurrencyPrefix(input: TextWithCursor): TextWithCursor {
        if (input.text.startsWith(config.currencySymbol)) {
            return input
        }

        val text =
            config.currencySymbol +
                input.text.trimStart { config.currencySymbol.toCharArray().contains(it) }
        return TextWithCursor(text, input.cursorPosition)
    }

    /** Of several decimal separators keeps the last one, the one the user has just typed. */
    private fun keepLastDecimalSeparator(input: TextWithCursor): TextWithCursor {
        val lastIndex = input.text.lastIndexOf(decimalSeparator)
        if (lastIndex == -1) {
            return input
        }

        val firstPart = input.text.substring(0, lastIndex)
        val secondPart = input.text.substring(lastIndex + 1)
        val removed = firstPart.count { it == decimalSeparator }
        val text =
            firstPart.replace(decimalSeparator.toString(), "") + decimalSeparator + secondPart
        return TextWithCursor(text, maxOf(0, input.cursorPosition - removed))
    }

    /** Rebuilds the number out of the bare digits, cutting the fractional part to size. */
    private fun buildNumber(
        input: TextWithCursor,
        newPartOfText: String?,
    ): TextWithCursor {
        val digits = input.text.replace("[^\\d$decimalSeparator]".toRegex(), "")
        val decimalSeparatorPos = digits.indexOf(decimalSeparator)
        var cursorPosition = cursorPositionWithoutGrouping(input)

        var integerPart =
            if (decimalSeparatorPos == -1) digits else digits.substring(0, decimalSeparatorPos)
        val fractionalPart =
            if (decimalSeparatorPos == -1) {
                ""
            } else {
                digits.substring(decimalSeparatorPos + 1).take(config.maxNumberOfDecimalPlaces)
            }

        if (integerPart.isEmpty() && (newPartOfText == "0" || fractionalPart.isNotEmpty())) {
            integerPart = "0"
            cursorPosition++
        }

        if (decimalSeparatorPos == -1 || config.maxNumberOfDecimalPlaces == 0) {
            return TextWithCursor(integerPart, cursorPosition)
        }

        if (integerPart.isEmpty()) {
            integerPart = "0"
            cursorPosition++
        }
        return TextWithCursor(
            integerPart + decimalSeparator + padFractionalPart(fractionalPart),
            cursorPosition,
        )
    }

    /** Cursor position in the text the grouping separators have been stripped from. */
    private fun cursorPositionWithoutGrouping(input: TextWithCursor): Int {
        val end = minOf(input.cursorPosition, input.text.length)
        val separatorsBeforeCursor = input.text.take(end).count { it == groupingSeparator }
        val separatorsInCurrencySymbol = config.currencySymbol.count { it == groupingSeparator }
        return input.cursorPosition - separatorsBeforeCursor + separatorsInCurrencySymbol
    }

    private fun padFractionalPart(fractionalPart: String): String {
        if (!config.decimalZerosPadding || fractionalPart.isEmpty()) {
            return fractionalPart
        }

        val missing = config.maxNumberOfDecimalPlaces - fractionalPart.length
        return if (missing > 0) fractionalPart + "0".repeat(missing) else fractionalPart
    }

    /** Assembles currency symbol, sign, grouped integer part and fractional part. */
    private fun composeResult(
        number: TextWithCursor,
        sign: String,
    ): TextWithCursor {
        val currencySymbol = config.currencySymbol
        val dotPos = number.text.indexOf(decimalSeparator)

        val textBeforeDot =
            if (dotPos == -1) number.text else number.text.substring(0, dotPos)
        val textAfterDot =
            if (dotPos == -1) "" else decimalSeparator + number.text.substring(dotPos + 1)

        val grouped =
            insertGroupingSeparators(
                integerText = textBeforeDot,
                cursorPosition = number.cursorPosition,
                prefixLength = currencySymbol.length + sign.length,
            )

        val result = currencySymbol + sign + grouped.text + textAfterDot
        val position =
            minOf(maxOf(grouped.cursorPosition, currencySymbol.length), result.length)

        return TextWithCursor(result, position)
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

    private data class SignedText(
        val sign: String,
        val value: TextWithCursor,
    )
}
