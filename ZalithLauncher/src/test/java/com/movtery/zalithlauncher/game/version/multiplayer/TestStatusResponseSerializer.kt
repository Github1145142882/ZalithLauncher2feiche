package com.movtery.zalithlauncher.game.version.multiplayer

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import org.junit.Test

class TestStatusResponseSerializer {

    @Test
    fun testSerializer1() {
        val element = buildJsonArray {
            addJsonObject {
                put("text", JsonPrimitive("123"))
            }
            addJsonObject {
                put("type", JsonPrimitive("text"))
                put("text", JsonPrimitive("abc"))
                put("color", JsonPrimitive("yellow"))
                put("extra", buildJsonArray {
                    addJsonObject {
                        put("text", JsonPrimitive("def"))
                        put("color", JsonPrimitive("red"))
                    }
                })
            }
            addJsonObject {
                put("text", JsonPrimitive("123"))
            }
            addJsonObject {
                put("text", JsonPrimitive("456"))
                put("color", JsonPrimitive("blue"))
            }
            addJsonObject {
                put("text", JsonPrimitive("789"))
            }
        }

        println("")
        println(element)
        val data = parseDescriptionFromJson(element)

        println("======================")

        println(data)
        println("")
    }

    @Test
    fun testSerializer2() {
        val element = buildJsonArray {
            add(JsonPrimitive("123"))
            addJsonObject {
                put("text", JsonPrimitive("123"))
                put("color", JsonPrimitive("red"))
                put("extra", buildJsonArray {
                    add(JsonPrimitive("456"))
                })
            }
            add(JsonPrimitive("abc"))
        }

        println("")
        println(element)
        val data = parseDescriptionFromJson(element)

        println("======================")

        println(data)
        println("")
    }
}