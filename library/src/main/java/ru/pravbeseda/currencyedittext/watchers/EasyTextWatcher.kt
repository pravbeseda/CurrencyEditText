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
package ru.pravbeseda.currencyedittext.watchers

import android.text.Editable
import android.text.TextWatcher

/**
 * Modified TextWatcher with disabled recursion
 * It is enough to use the method [onTextModified]
 * Original EasyTextWatcher: https://github.com/firmfreez/CurrencyEditText
 */
abstract class EasyTextWatcher : TextWatcher {
    // Flag to disable recursion
    private var ignore = false

    private var oldText: String? = null
    private var modifiedText: String? = null
    private var newPartOfText: String? = null

    // Cursor position after changing text
    private var editPosition: Int? = null

    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
        oldText = s.toString()
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        modifiedText = s.toString()
        newPartOfText = modifiedText!!.substring(start, start + count)
        editPosition = start + count
    }

    override fun afterTextChanged(s: Editable?) {
        if (!ignore) {
            startEditing()
            onTextModified(
                newPartOfText = newPartOfText,
                newText = modifiedText,
                oldText = oldText,
                editPosition = editPosition
            )
            endEditing()
        }
    }

    open fun startEditing() {
        ignore = true
    }

    open fun endEditing() {
        ignore = false
    }

    /**
     * By overriding this method to simplify the work with TextWatcher
     * @param newPartOfText changed part in text
     * @param newText new modified text
     * @param oldText old unchanged text
     * @param editPosition cursor position in new text
     */
    abstract fun onTextModified(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?
    )
}
