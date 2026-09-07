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

class CalculatorPlacementTest {
    @Test
    fun `a panel that fits below the field hangs below it, at its full height`() {
        assertEquals(
            CalculatorPlacement(above = false, height = null),
            CalculatorPlacement.choose(spaceBelow = 800, spaceAbove = 300, wanted = 700),
        )
    }

    @Test
    fun `a panel too tall for the space below flips above the field`() {
        assertEquals(
            CalculatorPlacement(above = true, height = null),
            CalculatorPlacement.choose(spaceBelow = 200, spaceAbove = 900, wanted = 700),
        )
    }

    @Test
    fun `a panel that fits neither side is capped to the roomier one`() {
        assertEquals(
            CalculatorPlacement(above = true, height = 500),
            CalculatorPlacement.choose(spaceBelow = 200, spaceAbove = 500, wanted = 700),
        )
        assertEquals(
            CalculatorPlacement(above = false, height = 400),
            CalculatorPlacement.choose(spaceBelow = 400, spaceAbove = 300, wanted = 700),
        )
    }

    @Test
    fun `equal room hangs the panel below, where a dropdown belongs`() {
        assertEquals(
            CalculatorPlacement(above = false, height = 300),
            CalculatorPlacement.choose(spaceBelow = 300, spaceAbove = 300, wanted = 700),
        )
    }
}
