/*
 * Copyright (c) Alexander Ivanov
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

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.BACKSPACE
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.CLEAR
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DECIMAL
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DIGIT_0
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DIGIT_1
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DIGIT_2
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DIGIT_3
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DIGIT_5
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.DIVIDE
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.MINUS
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.MULTIPLY
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.PLUS
import ru.pravbeseda.currencyedittext.calculator.CalculatorKey.SIGN
import java.math.BigDecimal

class CalculatorStateTest {
    @Test
    fun `digits append to the expression`() {
        assertEquals("123", press(DIGIT_1, DIGIT_2, DIGIT_3))
    }

    @Test
    fun `operators append between operands`() {
        assertEquals("1+2", press(DIGIT_1, PLUS, DIGIT_2))
    }

    @Test
    fun `a trailing operator is replaced rather than doubled`() {
        assertEquals("1*", press(DIGIT_1, PLUS, MULTIPLY))
    }

    @Test
    fun `an operator replaces a pending sign as well as the operator carrying it`() {
        assertEquals("1*", press(DIGIT_1, MULTIPLY, SIGN, MULTIPLY))
        assertEquals("1+", press(DIGIT_1, MULTIPLY, SIGN, PLUS))
    }

    @Test
    fun `an operator cannot turn a leading minus into another sign`() {
        assertEquals("-", press(MINUS, PLUS))
    }

    @Test
    fun `a leading minus starts a negative operand, other operators are ignored`() {
        assertEquals("-", press(MINUS))
        assertEquals("", press(PLUS))
        assertEquals("", press(MULTIPLY))
        assertEquals("", press(DIVIDE))
    }

    @Test
    fun `the decimal key opens a fraction and cannot be repeated in one operand`() {
        assertEquals("1.2", press(DIGIT_1, DECIMAL, DIGIT_2))
        assertEquals("1.2", press(DIGIT_1, DECIMAL, DIGIT_2, DECIMAL))
        assertEquals("1.2+3.5", press(DIGIT_1, DECIMAL, DIGIT_2, PLUS, DIGIT_3, DECIMAL, DIGIT_5))
    }

    @Test
    fun `the decimal key on an empty operand writes a leading zero`() {
        assertEquals("0.5", press(DECIMAL, DIGIT_5))
        assertEquals("1+0.5", press(DIGIT_1, PLUS, DECIMAL, DIGIT_5))
    }

    @Test
    fun `sign flips the operand being entered and flips it back`() {
        assertEquals("-100", press(DIGIT_1, DIGIT_0, DIGIT_0, SIGN))
        assertEquals("100", press(DIGIT_1, DIGIT_0, DIGIT_0, SIGN, SIGN))
    }

    @Test
    fun `sign flips only the last operand`() {
        assertEquals("100*-3", press(DIGIT_1, DIGIT_0, DIGIT_0, MULTIPLY, DIGIT_3, SIGN))
        assertEquals("100*3", press(DIGIT_1, DIGIT_0, DIGIT_0, MULTIPLY, DIGIT_3, SIGN, SIGN))
    }

    @Test
    fun `sign flips the preceding plus or minus instead of writing a double sign`() {
        assertEquals("100+50", press(DIGIT_1, DIGIT_0, DIGIT_0, MINUS, DIGIT_5, DIGIT_0, SIGN))
        assertEquals("100-50", press(DIGIT_1, DIGIT_0, DIGIT_0, PLUS, DIGIT_5, DIGIT_0, SIGN))
        assertEquals(BigDecimal("150.00"), value("100+50"))
    }

    @Test
    fun `flipping the preceding operator twice returns the original expression`() {
        assertEquals(
            "100-50",
            press(DIGIT_1, DIGIT_0, DIGIT_0, MINUS, DIGIT_5, DIGIT_0, SIGN, SIGN),
        )
    }

    @Test
    fun `sign after a trailing plus or minus flips that operator`() {
        assertEquals("100-", press(DIGIT_1, DIGIT_0, DIGIT_0, PLUS, SIGN))
        assertEquals("100+", press(DIGIT_1, DIGIT_0, DIGIT_0, MINUS, SIGN))
    }

    @Test
    fun `sign after a multiplication opens a negative operand, as no operator can absorb it`() {
        assertEquals("100*-", press(DIGIT_1, DIGIT_0, DIGIT_0, MULTIPLY, SIGN))
        assertEquals("100*", press(DIGIT_1, DIGIT_0, DIGIT_0, MULTIPLY, SIGN, SIGN))
    }

    @Test
    fun `sign on an empty expression starts a negative operand`() {
        assertEquals("-", press(SIGN))
        assertEquals("", press(SIGN, SIGN))
    }

    @Test
    fun `backspace removes one character and stops at an empty expression`() {
        assertEquals("1+", press(DIGIT_1, PLUS, DIGIT_2, BACKSPACE))
        assertEquals("", press(DIGIT_1, BACKSPACE, BACKSPACE))
    }

    @Test
    fun `clear empties the expression`() {
        assertEquals("", press(DIGIT_1, PLUS, DIGIT_2, CLEAR))
    }

    @Test
    fun `the panel opens seeded with the field's value`() {
        assertEquals("100.50", CalculatorState.seededWith(BigDecimal("100.50")).expression)
        assertEquals("-7", CalculatorState.seededWith(BigDecimal("-7")).expression)
    }

    @Test
    fun `a zero field seeds an empty expression rather than a stray zero`() {
        assertEquals("", CalculatorState.seededWith(BigDecimal.ZERO).expression)
        assertEquals("", CalculatorState.seededWith(BigDecimal("0.00")).expression)
    }

    @Test
    fun `the result is the evaluated expression at the field's scale`() {
        assertEquals(EvalResult.Success(BigDecimal("200.00")), result("100+2*50"))
        assertEquals(EvalResult.Failure, result("1+"))
        assertEquals(EvalResult.Failure, result(""))
    }

    @Test
    fun `a negative result is refused when the field allows no negative values`() {
        assertEquals(EvalResult.Success(BigDecimal("-5.00")), CalculatorState("1-6").result(scale = 2))
        assertEquals(
            EvalResult.Failure,
            CalculatorState("1-6").result(scale = 2, negativeValueAllow = false),
        )
        assertEquals(
            EvalResult.Success(BigDecimal("5.00")),
            CalculatorState("6-1").result(scale = 2, negativeValueAllow = false),
        )
    }

    @Test
    fun `the expression is displayed with the field's separator and the printed operators`() {
        assertEquals("1234,5\u00d72\u00f74", CalculatorState("1234.5*2/4").display(','))
        assertEquals("1234.5\u00d72", CalculatorState("1234.5*2").display('.'))
        assertEquals("\u22125+(\u22123)", CalculatorState("-5+(-3)").display('.'))
    }

    private fun press(vararg keys: CalculatorKey): String = keys.fold(CalculatorState()) { state, key -> state.press(key) }.expression

    private fun result(expression: String): EvalResult = CalculatorState(expression).result(scale = 2)

    private fun value(expression: String): BigDecimal = (result(expression) as EvalResult.Success).value
}
