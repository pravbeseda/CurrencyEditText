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
package ru.pravbeseda.currencyedittext.calculator

/**
 * A key of the calculator panel. [symbol] is what the key appends to the expression; for the three
 * editing keys, which append nothing, it is the label the panel draws on the button.
 */
internal enum class CalculatorKey(
    val symbol: Char,
) {
    DIGIT_0('0'),
    DIGIT_1('1'),
    DIGIT_2('2'),
    DIGIT_3('3'),
    DIGIT_4('4'),
    DIGIT_5('5'),
    DIGIT_6('6'),
    DIGIT_7('7'),
    DIGIT_8('8'),
    DIGIT_9('9'),
    DECIMAL('.'),
    PLUS('+'),
    MINUS('-'),
    MULTIPLY('*'),
    DIVIDE('/'),
    SIGN('±'),
    BACKSPACE('⌫'),
    CLEAR('C'),
}

/**
 * Digits and the decimal separator carry the value; every other key acts on it. The panel paints
 * the two roles in different colours, so each key belongs to exactly one of them.
 */
internal val CalculatorKey.isOperator: Boolean
    get() = !symbol.isDigit() && symbol != '.'

/** Operators the panel prints differently from the ASCII the expression is built out of. */
private val PRINTED_OPERATORS = mapOf('*' to '\u00d7', '/' to '\u00f7', '-' to '\u2212')

/** The character the user reads in place of [this]: an operator sign, or their decimal separator. */
internal fun Char.printed(decimalSeparator: Char): Char =
    when {
        this == '.' -> decimalSeparator
        else -> PRINTED_OPERATORS[this] ?: this
    }

/** The label the panel draws on the key's button. */
internal fun CalculatorKey.label(decimalSeparator: Char): String = symbol.printed(decimalSeparator).toString()
