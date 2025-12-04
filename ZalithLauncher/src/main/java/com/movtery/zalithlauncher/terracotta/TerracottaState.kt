package com.movtery.zalithlauncher.terracotta

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import com.google.gson.JsonParser
import com.google.gson.TypeAdapter
import com.google.gson.TypeAdapterFactory
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonWriter
import com.movtery.zalithlauncher.R
import com.movtery.zalithlauncher.terracotta.TerracottaState.Exception.Type
import com.movtery.zalithlauncher.terracotta.profile.TerracottaProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * [Modified from HMCL](https://github.com/HMCL-dev/HMCL/blob/bd6a6fa/HMCL/src/main/java/org/jackhuang/hmcl/terracotta/TerracottaState.java)
 */
sealed class TerracottaState {
    open fun isUIFakeState(): Boolean = false
    open fun isForkOf(state: TerracottaState?): Boolean = false

    object Bootstrap : TerracottaState()

    class Uninitialized(val hasLegacy: Boolean) : TerracottaState()

    class Preparing(initialProgress: Double = 0.0) : TerracottaState(), ITerracottaProviderContext {
        private val _progress = MutableStateFlow(initialProgress)
        val progress: StateFlow<Double> get() = _progress

        private val installFence = AtomicBoolean(false)

        fun setProgress(value: Double) {
            _progress.value = value
        }

        override fun requestInstallFence(): Boolean = installFence.compareAndSet(false, true)
        override fun hasInstallFence(): Boolean = !installFence.get()
    }

    class Launching : TerracottaState()

    sealed class PortSpecific(
        @Transient
        var port: Int
    ) : TerracottaState()

    sealed class Ready(
        port: Int,
        @SerializedName("index")
        val index: Int,
        @SerializedName("state")
        val state: String
    ) : PortSpecific(port) {
        override fun isUIFakeState(): Boolean = index == -1

        override fun toString(): String {
            val simple = javaClass.getSimpleName()
            val withUnderscore = simple.replace("([a-z])([A-Z])".toRegex(), "$1_$2")
            return withUnderscore.lowercase()
        }
    }

    class Unknown(port: Int) : PortSpecific(port)

    class Waiting(port: Int, index: Int, state: String) : Ready(port, index, state)
    class HostScanning(port: Int, index: Int, state: String) : Ready(port, index, state)
    class HostStarting(port: Int, index: Int, state: String) : Ready(port, index, state)

    class HostOK(
        port: Int,
        index: Int,
        state: String,
        @SerializedName("room")
        val code: String?,
        @SerializedName("profile_index")
        val profileIndex: Int,
        @SerializedName("profiles")
        val profiles: List<TerracottaProfile>?
    ) : Ready(port, index, state) {

        override fun isForkOf(state: TerracottaState?): Boolean =
            state is HostOK && (this.index - state.index) <= profileIndex
    }

    class GuestStarting(port: Int, index: Int, state: String) : Ready(port, index, state)

    class GuestOK(
        port: Int,
        index: Int,
        state: String,
        @SerializedName("url")
        val url: String?,
        @SerializedName("profile_index")
        val profileIndex: Int,
        @SerializedName("profiles")
        val profiles: List<TerracottaProfile>?
    ) : Ready(port, index, state) {

        override fun isForkOf(state: TerracottaState?): Boolean =
            state is GuestOK && (this.index - state.index) <= profileIndex
    }

    class Exception(port: Int, index: Int, state: String, val type: Int) : Ready(port, index, state) {
        enum class Type(val textRes: Int) {
            PING_HOST_FAIL(R.string.terracotta_status_exception_desc_ping_host_fail),
            PING_HOST_RST(R.string.terracotta_status_exception_desc_ping_host_rst),
            GUEST_ET_CRASH(R.string.terracotta_status_exception_desc_guest_et_crash),
            HOST_ET_CRASH(R.string.terracotta_status_exception_desc_host_et_crash),
            PING_SERVER_RST(R.string.terracotta_status_exception_desc_ping_server_rst),
            SCAFFOLDING_INVALID_RESPONSE(R.string.terracotta_status_exception_desc_scaffolding_invalid_response)
        }

        fun getEnumType(): Type = Type.entries[type]
    }

    class Fatal(val type: Type) : TerracottaState() {
        enum class Type {
            OS,
            NETWORK,
            INSTALL,
            TERRACOTTA,
            UNKNOWN
        }

        fun isRecoverable(): Boolean = type != Type.UNKNOWN
    }

    companion object {
        private fun createGson(): Gson = GsonBuilder()
            .registerTypeAdapterFactory(TerracottaStateTypeAdapterFactory())
            .create()

        val TerracottaStateGson = createGson()
    }
}

interface ITerracottaProviderContext {
    fun requestInstallFence(): Boolean
    fun hasInstallFence(): Boolean
}

class TerracottaStateTypeAdapterFactory : TypeAdapterFactory {
    override fun <T> create(gson: Gson, type: TypeToken<T>): TypeAdapter<T>? {
        val rawType = type.rawType
        if (!TerracottaState.Ready::class.java.isAssignableFrom(rawType)) return null

        val waitingAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.Waiting::class.java))
        val hostScanningAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.HostScanning::class.java))
        val hostStartingAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.HostStarting::class.java))
        val hostOKAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.HostOK::class.java))
        val guestStartingAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.GuestStarting::class.java))
        val guestOKAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.GuestOK::class.java))
        val exceptionAdapter = gson.getDelegateAdapter(this, TypeToken.get(TerracottaState.Exception::class.java))

        @Suppress("UNCHECKED_CAST")
        return object : TypeAdapter<TerracottaState.Ready>() {
            override fun write(out: JsonWriter, value: TerracottaState.Ready) {
                when (value) {
                    is TerracottaState.Waiting -> waitingAdapter.write(out, value)
                    is TerracottaState.HostScanning -> hostScanningAdapter.write(out, value)
                    is TerracottaState.HostStarting -> hostStartingAdapter.write(out, value)
                    is TerracottaState.HostOK -> hostOKAdapter.write(out, value)
                    is TerracottaState.GuestStarting -> guestStartingAdapter.write(out, value)
                    is TerracottaState.GuestOK -> guestOKAdapter.write(out, value)
                    is TerracottaState.Exception -> exceptionAdapter.write(out, value)
                }
            }

            override fun read(reader: JsonReader): TerracottaState.Ready {
                val jsonElement = JsonParser.parseReader(reader).asJsonObject
                val stateName = jsonElement.get("state")?.asString

                val result: TerracottaState.Ready = when (stateName) {
                    "waiting" -> waitingAdapter.fromJsonTree(jsonElement)
                    "host-scanning" -> hostScanningAdapter.fromJsonTree(jsonElement)
                    "host-starting" -> hostStartingAdapter.fromJsonTree(jsonElement)
                    "host-ok" -> hostOKAdapter.fromJsonTree(jsonElement)
                    "guest-starting" -> guestStartingAdapter.fromJsonTree(jsonElement)
                    "guest-ok" -> guestOKAdapter.fromJsonTree(jsonElement)
                    "exception" -> exceptionAdapter.fromJsonTree(jsonElement)
                    else -> throw JsonParseException("Unknown state type: $stateName")
                }

                //统一进行校验
                validateResult(result)

                return result
            }
        } as TypeAdapter<T>
    }

    private fun validateResult(result: TerracottaState.Ready) {
        when (result) {
            is TerracottaState.HostOK -> {
                require(result.code != null) { "HostOK.code is null" }
                require(result.profiles != null) { "HostOK.profiles is null" }
            }
            is TerracottaState.GuestOK -> {
                require(result.profiles != null) { "GuestOK.profiles is null" }
            }
            is TerracottaState.Exception -> {
                val type = result.type
                val max = Type.entries.size
                require(type in 0 until max) { "Exception.type=$type must be in [0, $max)" }
            }
            else -> {}
        }
    }
}
