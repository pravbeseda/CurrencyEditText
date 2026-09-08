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

import org.junit.Assert.assertEquals
import org.junit.Test

/** The panel paints the two roles differently, so every key belongs to exactly one of them. */
class CalculatorKeyRoleTest {
    private val value =
        setOf(
            CalculatorKey.DIGIT_0,
            CalculatorKey.DIGIT_1,
            CalculatorKey.DIGIT_2,
            CalculatorKey.DIGIT_3,
            CalculatorKey.DIGIT_4,
            CalculatorKey.DIGIT_5,
            CalculatorKey.DIGIT_6,
            CalculatorKey.DIGIT_7,
            CalculatorKey.DIGIT_8,
            CalculatorKey.DIGIT_9,
            CalculatorKey.DECIMAL,
        )

    @Test
    fun `digits and the decimal separator are the value, every other key acts on it`() {
        CalculatorKey.entries.forEach { key ->
            assertEquals("$key", key !in value, key.isOperator)
        }
    }
}
