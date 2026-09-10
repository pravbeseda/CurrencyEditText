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

class CalculatorKeyTest {
    @Test
    fun `a key is drawn with the printed operator, not the one it appends`() {
        assertEquals("×", CalculatorKey.MULTIPLY.label('.'))
        assertEquals("÷", CalculatorKey.DIVIDE.label('.'))
        assertEquals("−", CalculatorKey.MINUS.label('.'))
    }

    @Test
    fun `the decimal key is drawn with the field's separator`() {
        assertEquals(",", CalculatorKey.DECIMAL.label(','))
        assertEquals(".", CalculatorKey.DECIMAL.label('.'))
    }

    @Test
    fun `the parenthesis key is drawn as both brackets, since one key writes either`() {
        assertEquals("( )", CalculatorKey.PARENTHESIS.label('.'))
    }

    @Test
    fun `every other key is drawn with its own symbol`() {
        assertEquals("7", CalculatorKey.DIGIT_7.label(','))
        assertEquals("±", CalculatorKey.SIGN.label(','))
        assertEquals("C", CalculatorKey.CLEAR.label(','))
    }
}
