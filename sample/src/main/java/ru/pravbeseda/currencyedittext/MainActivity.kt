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
package ru.pravbeseda.currencyedittext

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import pravbeseda.databinding.ActivityMainBinding
import ru.pravbeseda.currencyedittext.CurrencyEditText.Companion.State
import ru.pravbeseda.currencyedittext.model.CurrencyFormatConfig
import ru.pravbeseda.currencyedittext.util.Routines.Companion.bigDecimalToString
import java.math.BigDecimal

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        applySystemBarInsets()

        binding.currencyEditText.setValue(BigDecimal(1123.476))
        binding.currencyMaterialEditText.setValue(BigDecimal(1432.167))

        binding.currencyEditText.setDecimalZerosPadding(true)
        binding.currencyMaterialEditText.setDecimalZerosPadding(true)
        binding.currencyMaterialEditText.setEmptyStringForZero(false)

        binding.currencyEditText.onValueChanged { _, state: State, textError: String ->
            if (state !== State.ERROR) {
                binding.textView.text = formatValue(binding.currencyEditText.value)
            } else {
                binding.textView.text = textError
            }
            binding.currencyMaterialEditText.validate()
        }

        binding.textView.text = formatValue(binding.currencyEditText.value)

        binding.button.setOnClickListener {
            binding.currencyEditText.text?.clear()
        }

        binding.currencyEditText.setValidator { value ->
            var error = ""
            if (value < BigDecimal(1000)) {
                error = "Value is less than 1000"
            }
            error
        }

        binding.currencyMaterialEditText.setValidator { value ->
            var error = ""
            if (value < binding.currencyEditText.value) {
                error = "Value can't be less than the first field"
            }
            error
        }
    }

    // Targeting SDK 35+ makes the window edge-to-edge: without this the system bars
    // are drawn over the content.
    private fun applySystemBarInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.toolbar.updatePadding(top = systemBars.top)
            binding.root.updatePadding(bottom = systemBars.bottom)
            insets
        }
    }

    private fun formatValue(value: BigDecimal): String =
        bigDecimalToString(
            value,
            CurrencyFormatConfig(
                decimalSeparator = '.',
                groupingSeparator = ' ',
                decimalLength = 2,
                showPlusSign = true,
            ),
        )
}
