/*
 * Copyright (C) 2017 JRummy Apps Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.jrummyapps.android.colorpicker

import androidx.annotation.ColorInt

/**
 * Callback used for getting the selected color from a color picker dialog.
 */
interface ColorPickerDialogListener {
    /**
     * Callback that is invoked when a color is selected from the color picker dialog.
     *
     * @param dialogId
     * The dialog id used to create the dialog instance.
     * @param newColor
     * The selected color
     */
    fun onColorSelected(dialogId: Int, @ColorInt newColor: Int)

    /**
     * Callback that is invoked when the color picker dialog was dismissed.
     *
     * @param dialogId
     * The dialog id used to create the dialog instance.
     */
    fun onDialogDismissed(dialogId: Int)
}
