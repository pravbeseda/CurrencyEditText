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
import java.math.BigDecimal

class ExpressionEvaluatorTest {
    @Test
    fun `multiplication binds tighter than addition`() {
        assertEquals(success("200.00"), evaluate("100+2*50", scale = 2))
    }

    @Test
    fun `parentheses override precedence`() {
        assertEquals(success("5100.00"), evaluate("(100+2)*50", scale = 2))
    }

    @Test
    fun `division binds tighter than subtraction`() {
        assertEquals(success("90.00"), evaluate("100-20/2", scale = 2))
    }

    @Test
    fun `leading minus is a unary sign`() {
        assertEquals(success("-2.00"), evaluate("-5+3", scale = 2))
    }

    @Test
    fun `unary minus applies after an operator`() {
        assertEquals(success("-15.00"), evaluate("5*-3", scale = 2))
    }

    @Test
    fun `unary minus applies inside parentheses`() {
        assertEquals(success("-8.00"), evaluate("2*(-7+3)", scale = 2))
    }

    @Test
    fun `decimals are exact, unlike double arithmetic`() {
        assertEquals(success("0.30"), evaluate("0.1+0.2", scale = 2))
    }

    @Test
    fun `multiplication is exact, however many digits the operands carry`() {
        assertEquals(success("89999999999999991.00"), evaluate("9999999999999999*9", scale = 2))
    }

    @Test
    fun `intermediate division keeps precision the final scale would lose`() {
        assertEquals(success("100.00"), evaluate("100/3*3", scale = 2))
    }

    @Test
    fun `the result is rounded half up to the requested scale`() {
        assertEquals(success("3.33"), evaluate("10/3", scale = 2))
        assertEquals(success("3.333"), evaluate("10/3", scale = 3))
        assertEquals(success("0.13"), evaluate("0.125", scale = 2))
    }

    @Test
    fun `a scale of zero rounds to whole units`() {
        assertEquals(success("3"), evaluate("10/3", scale = 0))
    }

    @Test
    fun `division by zero fails instead of throwing`() {
        assertEquals(EvalResult.Failure, evaluate("10/0", scale = 2))
        assertEquals(EvalResult.Failure, evaluate("10/(5-5)", scale = 2))
    }

    @Test
    fun `an unbalanced expression fails`() {
        assertEquals(EvalResult.Failure, evaluate("(1+2", scale = 2))
        assertEquals(EvalResult.Failure, evaluate("1+2)", scale = 2))
    }

    @Test
    fun `an incomplete expression fails`() {
        assertEquals(EvalResult.Failure, evaluate("1+", scale = 2))
        assertEquals(EvalResult.Failure, evaluate("", scale = 2))
        assertEquals(EvalResult.Failure, evaluate("()", scale = 2))
    }

    @Test
    fun `an unknown character fails`() {
        assertEquals(EvalResult.Failure, evaluate("1+a", scale = 2))
        assertEquals(EvalResult.Failure, evaluate("1,5+1", scale = 2))
    }

    @Test
    fun `a bare number is returned at the requested scale`() {
        assertEquals(success("42.00"), evaluate("42", scale = 2))
    }

    private fun evaluate(
        expression: String,
        scale: Int,
    ): EvalResult = ExpressionEvaluator.evaluate(expression, scale)

    private fun success(value: String): EvalResult = EvalResult.Success(BigDecimal(value))
}
