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

private val OPERATOR_KEYS =
    setOf(
        CalculatorKey.PLUS,
        CalculatorKey.MINUS,
        CalculatorKey.MULTIPLY,
        CalculatorKey.DIVIDE,
    )

/** Characters after which an operand — and therefore a unary minus — may start. */
private const val OPERAND_MAY_FOLLOW = "+-*/("

/** The binary operators, and therefore also the characters a pending sign can be written among. */
private const val OPERATORS = "+-*/"

private fun Char.isOperandChar(): Boolean = isDigit() || this == '.'

private fun Char?.mayPrecedeOperand(): Boolean = this != null && this in OPERAND_MAY_FOLLOW

private fun String.withCharAt(
    index: Int,
    char: Char,
): String = substring(0, index) + char + substring(index + 1)

/**
 * What the calculator panel has been given so far, as a canonical ASCII expression. Holds no
 * reference to a view and knows nothing about locale: the panel translates the user's decimal
 * separator into `.` on the way in, and draws `*` and `/` as `×` and `÷` on the way out.
 *
 * Every key press returns a new state; a press that would make the expression unreadable — a
 * second decimal point in one operand, a digit glued to a closing parenthesis — returns the state
 * unchanged rather than a mess the user has to back out of.
 */
internal data class CalculatorState(
    val expression: String = "",
) {
    fun press(key: CalculatorKey): CalculatorState =
        when (key) {
            CalculatorKey.CLEAR -> CalculatorState()
            CalculatorKey.BACKSPACE -> CalculatorState(expression.dropLast(1))
            CalculatorKey.SIGN -> flipSign()
            CalculatorKey.DECIMAL -> appendDecimal()
            CalculatorKey.OPEN_PAREN -> appendOpeningParenthesis()
            CalculatorKey.CLOSE_PAREN -> appendClosingParenthesis()
            in OPERATOR_KEYS -> appendOperator(key.symbol)
            else -> appendDigit(key.symbol)
        }

    fun result(scale: Int): EvalResult = ExpressionEvaluator.evaluate(expression, scale)

    private val lastChar: Char? get() = expression.lastOrNull()

    /** The run of digits and decimal points at the end — what the user is typing right now. */
    private val currentOperand: String get() = expression.takeLastWhile { it.isOperandChar() }

    private fun appendDigit(digit: Char): CalculatorState = if (lastChar == ')') this else CalculatorState(expression + digit)

    private fun appendDecimal(): CalculatorState =
        when {
            lastChar == ')' || currentOperand.contains('.') -> this
            currentOperand.isEmpty() -> CalculatorState("${expression}0.")
            else -> CalculatorState("$expression.")
        }

    /**
     * Replaces everything still pending at the end — an operator, and the sign `flipSign` may have
     * written after it — so that `1*-` plus `+` is `1+` and never `1*+`. Where no operand has
     * started yet, only a minus is legal, and any other operator leaves the expression alone.
     */
    private fun appendOperator(operator: Char): CalculatorState {
        val pending = expression.takeLastWhile { it in OPERATORS }
        val settled = expression.dropLast(pending.length)
        return when {
            settled.isEmpty() || settled.last() == '(' -> {
                if (operator == '-') CalculatorState("$settled-") else this
            }

            else -> {
                CalculatorState(settled + operator)
            }
        }
    }

    private fun appendOpeningParenthesis(): CalculatorState =
        if (expression.isEmpty() || lastChar.mayPrecedeOperand()) {
            CalculatorState("$expression(")
        } else {
            this
        }

    private fun appendClosingParenthesis(): CalculatorState {
        val unclosed = expression.count { it == '(' } - expression.count { it == ')' }
        val operandFinished = lastChar?.let { it.isDigit() || it == ')' } == true
        return if (unclosed > 0 && operandFinished) CalculatorState("$expression)") else this
    }

    /**
     * Flips the sign of the operand being entered. A unary `-` in front of that operand is
     * removed; a binary `+` or `-` in front of it is flipped instead, so that `100-50` becomes
     * `100+50` rather than the unreadable `100--50`. Only `*` and `/`, which cannot absorb a sign,
     * get a unary minus written after them.
     */
    private fun flipSign(): CalculatorState {
        if (lastChar == ')') return this
        val start = expression.length - currentOperand.length
        val preceding = expression.getOrNull(start - 1)
        return when {
            isUnaryMinusBefore(start) -> CalculatorState(expression.removeRange(start - 1, start))
            preceding == '+' -> CalculatorState(expression.withCharAt(start - 1, '-'))
            preceding == '-' -> CalculatorState(expression.withCharAt(start - 1, '+'))
            else -> CalculatorState(expression.substring(0, start) + '-' + currentOperand)
        }
    }

    private fun isUnaryMinusBefore(start: Int): Boolean =
        start > 0 &&
            expression[start - 1] == '-' &&
            (start == 1 || expression[start - 2] in OPERAND_MAY_FOLLOW)

    companion object {
        /**
         * Seeds the panel with the field's value. A zero field seeds nothing: the user is about to
         * type a number, not to build on a `0` they never entered.
         */
        fun seededWith(value: BigDecimal): CalculatorState =
            if (value.signum() == 0) CalculatorState() else CalculatorState(value.toPlainString())
    }
}
