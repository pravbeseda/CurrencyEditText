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

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

internal sealed interface EvalResult {
    data class Success(
        val value: BigDecimal,
    ) : EvalResult

    data object Failure : EvalResult
}

/**
 * Evaluates an arithmetic expression written with ASCII `+ - * / ( ) .` and digits.
 * Locale-specific separators are the caller's business: the panel translates what the user
 * typed into this canonical form.
 */
internal object ExpressionEvaluator {
    fun evaluate(
        expression: String,
        scale: Int,
    ): EvalResult {
        val parser = Parser(expression)
        val value = parser.parseSum()
        return if (value == null || !parser.atEnd) {
            EvalResult.Failure
        } else {
            EvalResult.Success(value.setScale(scale, RoundingMode.HALF_UP))
        }
    }
}

/**
 * Significant digits a division keeps. Addition, subtraction and multiplication are exact and take
 * no context at all; only division has to be told when to stop, or `10 / 3` never terminates. The
 * figure is wider than any field's scale on purpose, so that a division in the middle of an
 * expression does not round away digits a later multiplication needs back: `100 / 3 * 3` has to
 * end at 100.00, not 99.99.
 */
private const val INTERMEDIATE_PRECISION = 16

private val INTERMEDIATE_MATH = MathContext(INTERMEDIATE_PRECISION, RoundingMode.HALF_UP)

/**
 * Recursive descent over the grammar
 * `sum := product (('+' | '-') product)*`,
 * `product := factor (('*' | '/') factor)*`,
 * `factor := '-' factor | '(' sum ')' | number`.
 * Precedence and the unary minus fall out of the grammar. A `null` anywhere means the input is
 * malformed or divides by zero, and it propagates: once an operand fails to parse the loops stop
 * and the whole expression is null.
 */
private class Parser(
    private val input: String,
) {
    private var pos = 0

    val atEnd: Boolean get() = pos == input.length

    private fun nextIsOneOf(operators: String): Boolean = !atEnd && input[pos] in operators

    fun parseSum(): BigDecimal? {
        var left = parseProduct()
        while (left != null && nextIsOneOf("+-")) {
            val plus = input[pos] == '+'
            pos++
            val right = parseProduct()
            left =
                when {
                    right == null -> null
                    plus -> left.add(right)
                    else -> left.subtract(right)
                }
        }
        return left
    }

    private fun parseProduct(): BigDecimal? {
        var left = parseFactor()
        while (left != null && nextIsOneOf("*/")) {
            val times = input[pos] == '*'
            pos++
            val right = parseFactor()
            left =
                when {
                    right == null -> null
                    times -> left.multiply(right)
                    right.signum() == 0 -> null
                    else -> left.divide(right, INTERMEDIATE_MATH)
                }
        }
        return left
    }

    private fun parseFactor(): BigDecimal? {
        if (atEnd) return null
        return when {
            input[pos] == '-' -> {
                pos++
                parseFactor()?.negate()
            }

            input[pos] == '(' -> {
                pos++
                parseParenthesised()
            }

            else -> {
                parseNumber()
            }
        }
    }

    private fun parseParenthesised(): BigDecimal? {
        val value = parseSum()
        val closes = value != null && !atEnd && input[pos] == ')'
        if (!closes) return null
        pos++
        return value
    }

    private fun parseNumber(): BigDecimal? {
        val start = pos
        while (!atEnd && (input[pos].isDigit() || input[pos] == '.')) pos++
        return input.substring(start, pos).toBigDecimalOrNull()
    }
}
