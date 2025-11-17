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

package com.movtery.zalithlauncher.game.download.modpack.platform.mcbbs

import com.google.gson.annotations.SerializedName
import com.movtery.zalithlauncher.game.addons.modloader.ModLoader
import com.movtery.zalithlauncher.game.download.modpack.platform.PackManifest
import com.movtery.zalithlauncher.game.versioninfo.models.GameManifest

data class MCBBSManifest(
    val manifestType: String,
    val manifestVersion: Int,
    val name: String,
    val version: String,
    val author: String,
    val description: String,
    val fileApi: String?,
    val url: String,
    val forceUpdate: Boolean,
    val origins: List<Origin>,
    val addons: List<Addon>,
    val libraries: List<GameManifest.Library>,
    val files: List<AddonFile>,
    val settings: Settings,
    val launchInfo: LaunchInfo
): PackManifest {

    data class Origin(
        val type: String,
        val id: Int
    )

    data class Addon(
        val id: String,
        val version: String
    )

    data class Settings(
        @SerializedName("install_mods")
        val installMods: Boolean,

        @SerializedName("install_resourcepack")
        val installResourcepack: Boolean
    )

    data class AddonFile(
        val force: Boolean,
        val path: String,
        val hash: String
    )

    data class LaunchInfo(
        val minMemory: Int,
        val supportJava: List<Int>?,

        @SerializedName("launchArgument")
        val launchArguments: List<String>?,

        @SerializedName("javaArgument")
        val javaArguments: List<String>?
    )

    data class ServerInfo(
        val authlibInjectorServer: String?
    )

    /**
     * 尝试获取 Minecraft 版本
     */
    fun getMinecraftVersion(): String? =
        addons.find { it.id == "game" }?.version

    /**
     * 匹配模组加载器与版本
     */
    fun Addon.retrieveLoader(): Pair<ModLoader, String>? {
        return when (id) {
            "forge" -> ModLoader.FORGE to version
            "neoforge" -> ModLoader.NEOFORGE to version
            "fabric" -> ModLoader.FABRIC to version
            "quilt" -> ModLoader.QUILT to version
            "optifine" -> ModLoader.OPTIFINE to version
            else -> null
        }
    }
}