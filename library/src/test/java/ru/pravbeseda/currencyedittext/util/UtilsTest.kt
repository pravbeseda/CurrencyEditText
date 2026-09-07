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
package ru.pravbeseda.currencyedittext.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UtilsTest {
    @Test
    fun `Issue #23 - a field holding no number reads as zero`() {
        listOf("", " ", "$ ", "-", "abc").forEach { value ->
            assertEquals(
                "\"$value\" should read as zero",
                0,
                parseMoneyValueWithLocale(value, ',', '.', "$ ").toInt(),
            )
        }
    }

    @Test
    fun `a field holding a number reads as that number`() {
        assertEquals(1234.56, parseMoneyValueWithLocale("$ 1,234.56", ',', '.', "$ ").toDouble(), 0.0)
    }
}
