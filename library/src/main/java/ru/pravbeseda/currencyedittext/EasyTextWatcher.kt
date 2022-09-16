package ru.pravbeseda.currencyedittext

import android.text.Editable
import android.text.TextWatcher

/**
 * Упрощенный TextWatcher с отключенной рекурсией
 * в котором достаточно переопределить метод [onTextModified]
 */
abstract class EasyTextWatcher: TextWatcher {
    // Флаг для отключения рекурсии
    private var ignore = false

    // Текст до изменения
    private var oldText: String? = null

    // Изиененный текст
    private var modifiedText: String? = null

    // Новая часть измененного текста
    private var newPartOfText: String? = null

    // Позиция курсора после изменения
    private var editPosition: Int? = null

    override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        oldText = p0.toString()
    }

    override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        modifiedText = p0.toString()
        newPartOfText = p0?.substring(p1, p1 + p3)
        editPosition = p1 + p3
    }

    override fun afterTextChanged(p0: Editable?) {
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

    /**
     * Метод устанавливает флаг отключения рекурсии в активное состояние
     * (Нужен для красоты)
     */
    open fun startEditing() {
        ignore = true
    }

    /**
     * Метод выключает флаг отключения рекурсии
     * (Нужен для красоты)
     */
    open fun endEditing() {
        ignore = false
    }

    /**
     * Переопределив этот метод порой можно решить все проблемы с TextWatcher-ом
     * @param newPartOfText измененная часть в тексте
     * @param newText новый измененный текст
     * @param oldText старый неизмененный текст
     * @param editPosition позиция курсора в новом тексте
     */
    abstract fun onTextModified(newPartOfText: String?, newText: String?, oldText: String?, editPosition: Int?)
}