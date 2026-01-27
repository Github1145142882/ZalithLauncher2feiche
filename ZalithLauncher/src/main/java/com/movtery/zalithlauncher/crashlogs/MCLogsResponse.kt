package com.movtery.zalithlauncher.crashlogs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MCLogsResponse(
    @SerialName("success")
    val success: Boolean,
    @SerialName("url")
    val url: String? = null,
    @SerialName("id")
    val id: String? = null,
    @SerialName("error")
    val error: String? = null
)