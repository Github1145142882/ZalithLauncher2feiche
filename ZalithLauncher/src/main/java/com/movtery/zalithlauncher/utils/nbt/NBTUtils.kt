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

package com.movtery.zalithlauncher.utils.nbt

import net.querz.nbt.tag.ByteTag
import net.querz.nbt.tag.CompoundTag
import net.querz.nbt.tag.DoubleTag
import net.querz.nbt.tag.FloatTag
import net.querz.nbt.tag.IntTag
import net.querz.nbt.tag.ListTag
import net.querz.nbt.tag.LongTag
import net.querz.nbt.tag.ShortTag
import net.querz.nbt.tag.StringTag
import net.querz.nbt.tag.Tag

private inline fun <reified T : Tag<*>, R> CompoundTag.getAs(key: String, crossinline mapper: (T) -> R, defaultValue: R): R {
    return (this.get(key) as? T)?.let(mapper) ?: defaultValue
}

/** 获取指定键对应的 CompoundTag */
fun CompoundTag.asCompoundTag(key: String): CompoundTag? {
    return this.get(key) as? CompoundTag
}

/**
 * 获取指定键对应的 Boolean 值，通过 ByteTag 的方式手动判断
 */
fun CompoundTag.asBoolean(key: String, defaultValue: Boolean): Boolean {
    return asByte(key, if (defaultValue) 1 else 0) == 1.toByte()
}

/** 获取指定键对应的 Byte 值 */
fun CompoundTag.asByte(key: String, defaultValue: Byte?): Byte? {
    return getAs<ByteTag, Byte?>(key, { it.asByte() }, defaultValue)
}

/** 获取指定键对应的 Byte 值 */
fun CompoundTag.asByteNotNull(key: String, defaultValue: Byte): Byte {
    return getAs<ByteTag, Byte>(key, { it.asByte() }, defaultValue)
}

/** 获取指定键对应的 Short 值 */
fun CompoundTag.asShort(key: String, defaultValue: Short?): Short? {
    return getAs<ShortTag, Short?>(key, { it.asShort() }, defaultValue)
}

/** 获取指定键对应的 Short 值 */
fun CompoundTag.asShortNotNull(key: String, defaultValue: Short): Short {
    return getAs<ShortTag, Short>(key, { it.asShort() }, defaultValue)
}

/** 获取指定键对应的 Int 值 */
fun CompoundTag.asInt(key: String, defaultValue: Int?): Int? {
    return getAs<IntTag, Int?>(key, { it.asInt() }, defaultValue)
}

/** 获取指定键对应的 Int 值 */
fun CompoundTag.asIntNotNull(key: String, defaultValue: Int): Int {
    return getAs<IntTag, Int>(key, { it.asInt() }, defaultValue)
}

/** 获取指定键对应的 Long 值 */
fun CompoundTag.asLong(key: String, defaultValue: Long?): Long? {
    return getAs<LongTag, Long?>(key, { it.asLong() }, defaultValue)
}

/** 获取指定键对应的 Long 值 */
fun CompoundTag.asLongNotNull(key: String, defaultValue: Long): Long {
    return getAs<LongTag, Long>(key, { it.asLong() }, defaultValue)
}

/** 获取指定键对应的 Float 值 */
fun CompoundTag.asFloat(key: String, defaultValue: Float?): Float? {
    return getAs<FloatTag, Float?>(key, { it.asFloat() }, defaultValue)
}

/** 获取指定键对应的 Float 值 */
fun CompoundTag.asFloatNotNull(key: String, defaultValue: Float): Float {
    return getAs<FloatTag, Float>(key, { it.asFloat() }, defaultValue)
}

/** 获取指定键对应的 Double 值 */
fun CompoundTag.asDouble(key: String, defaultValue: Double?): Double? {
    return getAs<DoubleTag, Double?>(key, { it.asDouble() }, defaultValue)
}

/** 获取指定键对应的 Double 值 */
fun CompoundTag.asDoubleNotNull(key: String, defaultValue: Double): Double {
    return getAs<DoubleTag, Double>(key, { it.asDouble() }, defaultValue)
}

/** 获取指定键对应的 String 值 */
fun CompoundTag.asString(key: String, defaultValue: String?): String? {
    return getAs<StringTag, String?>(key, { it.value }, defaultValue)
}

/** 获取指定键对应的 String 值 */
fun CompoundTag.asStringNotNull(key: String, defaultValue: String): String {
    return getAs<StringTag, String>(key, { it.value }, defaultValue)
}

/** 获取指定键对应的 tag 列表 */
fun CompoundTag.asList(key: String): ListTag<CompoundTag>? {
    return getListTag(key)?.asCompoundTagList()
}
