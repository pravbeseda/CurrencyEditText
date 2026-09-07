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
 * Where the panel hangs and how tall it may be. A [height] of `null` means it may take the height
 * it asked for; a number is the room it has to fit into, which the panel then scrolls inside.
 */
internal data class CalculatorPlacement(
    val above: Boolean,
    val height: Int?,
) {
    companion object {
        /**
         * Puts the panel below the field where it fits there, above it where only that side has
         * room, and on the roomier side capped to that room where neither is tall enough — a
         * landscape screen, typically. An uncapped panel would be clipped to the space below the
         * field and could show no keys at all.
         */
        fun choose(
            spaceBelow: Int,
            spaceAbove: Int,
            wanted: Int,
        ): CalculatorPlacement {
            val above = wanted > spaceBelow && spaceAbove > spaceBelow
            val room = if (above) spaceAbove else spaceBelow
            return CalculatorPlacement(above = above, height = if (wanted <= room) null else room)
        }
    }
}
