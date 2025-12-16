/*
 * Zalith Launcher 2
 * Copyright (C) 2025 MovTery <movtery228@qq.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/gpl-3.0.txt>.
 */

package com.movtery.zalithlauncher.ui.screens.content.settings.layouts

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.setting.unit.BooleanSettingUnit
import com.movtery.zalithlauncher.setting.unit.EnumSettingUnit
import com.movtery.zalithlauncher.setting.unit.IntSettingUnit
import com.movtery.zalithlauncher.setting.unit.NullableIntSettingUnit
import com.movtery.zalithlauncher.setting.unit.StringSettingUnit
import com.movtery.zalithlauncher.setting.unit.getOrMin
import com.movtery.zalithlauncher.setting.unit.min
import com.movtery.zalithlauncher.ui.components.SimpleIntSliderLayout
import com.movtery.zalithlauncher.ui.components.SimpleListLayout
import com.movtery.zalithlauncher.ui.components.SwitchLayout
import com.movtery.zalithlauncher.ui.components.TextInputLayout
import com.movtery.zalithlauncher.ui.components.TitleAndSummary
import com.movtery.zalithlauncher.utils.animation.getAnimateTween
import kotlin.enums.EnumEntries

@DslMarker
annotation class SettingsLayoutDsl

@SettingsLayoutDsl
class SettingsLayoutScope {
    @Composable
    fun SwitchSettingsLayout(
        modifier: Modifier = Modifier,
        unit: BooleanSettingUnit,
        title: String,
        summary: String? = null,
        enabled: Boolean = true,
        verticalAlignment: Alignment.Vertical = Alignment.Top,
        onCheckedChange: (Boolean) -> Unit = {},
        trailingIcon: @Composable (RowScope.() -> Unit)? = null,
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
    ) {
        SwitchLayout(
            checked = unit.state,
            onCheckedChange = { value ->
                unit.save(value)
                onCheckedChange(value)
            },
            modifier = modifier,
            title = title,
            summary = summary,
            enabled = enabled,
            verticalAlignment = verticalAlignment,
            trailingIcon = trailingIcon,
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }

    @Composable
    fun SliderSettingsLayout(
        modifier: Modifier = Modifier,
        unit: IntSettingUnit,
        title: String,
        summary: String? = null,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        suffix: String? = null,
        onValueChange: (Int) -> Unit = {},
        enabled: Boolean = true,
        fineTuningControl: Boolean = false,
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
    ) {
        var value by rememberSaveable { mutableIntStateOf(unit.getValue()) }

        SimpleIntSliderLayout(
            modifier = modifier,
            value = unit.state,
            title = title,
            summary = summary,
            valueRange = valueRange,
            steps = steps,
            suffix = suffix,
            onValueChange = {
                value = it
                unit.updateState(it)
                onValueChange(it)
            },
            onValueChangeFinished = { unit.save(value) },
            enabled = enabled,
            fineTuningControl = fineTuningControl,
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }

    @Composable
    fun SliderSettingsLayout(
        modifier: Modifier = Modifier,
        unit: NullableIntSettingUnit,
        title: String,
        summary: String? = null,
        valueRange: ClosedFloatingPointRange<Float>,
        steps: Int = 0,
        suffix: String? = null,
        onValueChange: (Int) -> Unit = {},
        enabled: Boolean = true,
        fineTuningControl: Boolean = false,
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
    ) {
        var value by rememberSaveable { mutableIntStateOf(unit.getOrMin()) }

        SimpleIntSliderLayout(
            modifier = modifier,
            value = unit.state ?: unit.min,
            title = title,
            summary = summary,
            valueRange = valueRange,
            steps = steps,
            suffix = suffix,
            onValueChange = {
                value = it
                unit.updateState(it)
                onValueChange(it)
            },
            onValueChangeFinished = { unit.save(value) },
            enabled = enabled,
            fineTuningControl = fineTuningControl,
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }

    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    fun <E: Enum<E>> EnumSettingsLayout(
        modifier: Modifier = Modifier,
        unit: EnumSettingUnit<E>,
        entries: EnumEntries<E>,
        title: String,
        summary: String? = null,
        getRadioText: @Composable (E) -> String,
        getRadioEnable: (E) -> Boolean,
        maxItemsInEachRow: Int = Int.MAX_VALUE,
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall,
        onRadioClick: (E) -> Unit = {},
        onValueChange: (E) -> Unit = {}
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(all = 8.dp)
                .padding(bottom = 4.dp)
        ) {
            TitleAndSummary(
                title = title,
                summary = summary,
                titleStyle = titleStyle,
                summaryStyle = summaryStyle
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = getAnimateTween()),
                horizontalArrangement = Arrangement.SpaceEvenly,
                maxItemsInEachRow = maxItemsInEachRow
            ) {
                entries.forEach { enum ->
                    Row {
                        val radioText = getRadioText(enum)
                        RadioButton(
                            enabled = getRadioEnable(enum),
                            selected = unit.state == enum,
                            onClick = {
                                onRadioClick(enum)
                                if (unit.state == enum) return@RadioButton
                                unit.save(enum)
                                onValueChange(enum)
                            }
                        )
                        Text(
                            text = radioText,
                            style = MaterialTheme.typography.labelMedium,
                            modifier = Modifier
                                .align(Alignment.CenterVertically)
                                .alpha(alpha = if (getRadioEnable(enum)) 1f else 0.5f)
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun <E> ListSettingsLayout(
        modifier: Modifier = Modifier,
        unit: StringSettingUnit,
        items: List<E>,
        title: String,
        summary: String? = null,
        getItemText: @Composable (E) -> String,
        getItemId: (E) -> String,
        getItemSummary: (@Composable (E) -> Unit)? = null,
        enabled: Boolean = true,
        itemListPadding: PaddingValues = PaddingValues(bottom = 4.dp),
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall,
        onValueChange: (E) -> Unit = {}
    ) {
        SimpleListLayout(
            modifier = modifier,
            items = items,
            currentId = unit.state,
            defaultId = unit.defaultValue,
            title = title,
            summary = summary,
            getItemText = getItemText,
            getItemId = getItemId,
            getItemSummary = getItemSummary,
            enabled = enabled,
            itemListPadding = itemListPadding,
            onValueChange = { item ->
                unit.save(getItemId(item))
                onValueChange(item)
            },
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }

    @Composable
    fun <E: Enum<E>> ListSettingsLayout(
        modifier: Modifier = Modifier,
        unit: EnumSettingUnit<E>,
        items: List<E>,
        title: String,
        summary: String? = null,
        getItemText: @Composable (E) -> String,
        getItemSummary: (@Composable (E) -> Unit)? = null,
        enabled: Boolean = true,
        itemListPadding: PaddingValues = PaddingValues(bottom = 4.dp),
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall,
        onValueChange: (E) -> Unit = {}
    ) {
        SimpleListLayout(
            modifier = modifier,
            items = items,
            currentId = unit.state.name,
            defaultId = unit.defaultValue.name,
            title = title,
            summary = summary,
            getItemText = getItemText,
            getItemId = { it.name },
            getItemSummary = getItemSummary,
            enabled = enabled,
            itemListPadding = itemListPadding,
            onValueChange = { item ->
                unit.save(item)
                onValueChange(item)
            },
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }

    @Composable
    fun TextInputSettingsLayout(
        modifier: Modifier = Modifier,
        unit: StringSettingUnit,
        title: String,
        summary: String? = null,
        label: String? = null,
        onValueChange: (String) -> Unit = {},
        singleLine: Boolean = true,
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall
    ) {
        TextInputLayout(
            modifier = modifier,
            currentValue = unit.getValue(),
            title = title,
            summary = summary,
            onValueChange = { value ->
                unit.save(value)
                onValueChange(value)
            },
            label = {
                Text(text = label ?: stringResource(R.string.settings_label_ignore_if_blank))
            },
            singleLine = singleLine,
            titleStyle = titleStyle,
            summaryStyle = summaryStyle
        )
    }

    @Composable
    fun ClickableSettingsLayout(
        modifier: Modifier = Modifier,
        title: String,
        summary: String? = null,
        enabled: Boolean = true,
        titleStyle: TextStyle = MaterialTheme.typography.titleSmall,
        summaryStyle: TextStyle = MaterialTheme.typography.labelSmall,
        onClick: () -> Unit = {}
    ) {
        Column(
            modifier = modifier
                .clip(shape = RoundedCornerShape(22.0.dp))
                .clickable(onClick = onClick, enabled = enabled)
                .padding(horizontal = 8.dp, vertical = 12.dp)
                .alpha(if (enabled) 1f else 0.5f)
        ) {
            TitleAndSummary(
                title = title,
                summary = summary,
                titleStyle = titleStyle,
                summaryStyle = summaryStyle
            )
        }
    }
}