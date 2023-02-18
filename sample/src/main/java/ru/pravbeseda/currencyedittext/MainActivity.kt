/**
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
import java.math.BigDecimal
import kotlinx.android.synthetic.main.activity_main.*
import pravbeseda.R
import ru.pravbeseda.currencyedittext.CurrencyEditText.Companion.State

class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currencyEditText.setValue(BigDecimal(123.476))
        currencyMaterialEditText.setValue(BigDecimal(432.167))

        currencyEditText.onValueChanged { bigDecimal, state: State, textError: String ->
            if (state !== State.ERROR) {
                textView.text = bigDecimal.toString()
            } else {
                textView.text = textError
            }
            currencyMaterialEditText.validate()
        }

        textView.text = currencyEditText.getValue().toString()

        button.setOnClickListener {
            currencyEditText.text?.clear()
        }

        currencyMaterialEditText.hint = "Enter value"

        currencyEditText.setValidator { value ->
            var error = ""
            if (value < BigDecimal(1000)) {
                error = "Value is less than 1000"
            }
            error
        }

        currencyMaterialEditText.setValidator { value ->
            var error = ""
            if (value < currencyEditText.value) {
                error = "Value can't be less than the first field"
            }
            error
        }
    }
}
