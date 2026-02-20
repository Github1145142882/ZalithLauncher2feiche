package com.movtery.zalithlauncher.ui.screens.content.versions.export

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.game.version.export.PackType
import com.movtery.zalithlauncher.info.InfoDistributor
import com.movtery.zalithlauncher.ui.base.BaseScreen
import com.movtery.zalithlauncher.ui.components.AnimatedLazyColumn
import com.movtery.zalithlauncher.ui.components.BackgroundCard
import com.movtery.zalithlauncher.ui.screens.NestedNavKey
import com.movtery.zalithlauncher.ui.screens.NormalNavKey

/**
 * 整合包导出类型选择页
 */
@Composable
fun ExportTypeSelectScreen(
    mainScreenKey: NavKey?,
    exportScreenKey: NavKey?,
    onTypeSelect: (PackType) -> Unit
) {
    BaseScreen(
        levels1 = listOf(
            Pair(NestedNavKey.VersionExport::class.java, mainScreenKey)
        ),
        Triple(NormalNavKey.VersionExports.SelectType, exportScreenKey, false)
    ) { isVisible ->
        AnimatedLazyColumn(
            isVisible = isVisible,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(all = 34.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) { scope ->
            //MCBBS
            animatedItem(scope) { yOffset ->
                TypeItem(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    title = stringResource(R.string.versions_export_type_mcbbs),
                    summary = stringResource(R.string.versions_export_type_mcbbs_summary, InfoDistributor.LAUNCHER_SHORT_NAME),
                    icon = painterResource(R.drawable.img_chest),
                    onClick = { onTypeSelect(PackType.MCBBS) }
                )
            }

            //Modrinth
            animatedItem(scope) { yOffset ->
                TypeItem(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    title = stringResource(R.string.versions_export_type_modrinth),
                    summary = stringResource(R.string.versions_export_type_modrinth_summary),
                    icon = painterResource(R.drawable.img_platform_modrinth),
                    onClick = { onTypeSelect(PackType.Modrinth) }
                )
            }

            //MultiMC
            animatedItem(scope) { yOffset ->
                TypeItem(
                    modifier = Modifier
                        .offset { IntOffset(x = 0, y = yOffset.roundToPx()) },
                    title = stringResource(R.string.versions_export_type_multimc),
                    summary = stringResource(R.string.versions_export_type_multimc_summary, InfoDistributor.LAUNCHER_SHORT_NAME),
                    icon = painterResource(R.drawable.img_platform_multimc),
                    onClick = { onTypeSelect(PackType.MultiMC) }
                )
            }
        }
    }
}


/**
 * 导出类型布局
 */
@Composable
private fun TypeItem(
    title: String,
    summary: String,
    icon: Painter,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    BackgroundCard(
        modifier = modifier,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            //图标
            Image(
                modifier = Modifier.size(34.dp),
                painter = icon,
                contentDescription = title,
                contentScale = ContentScale.FillBounds
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )

                Text(
                    modifier = Modifier.alpha(0.7f),
                    text = summary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowRight,
                contentDescription = null
            )
        }
    }
}