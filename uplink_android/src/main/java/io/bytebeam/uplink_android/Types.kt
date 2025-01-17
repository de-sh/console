import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue
import org.json.JSONObject

@Parcelize
data class UplinkConfig(
    val credentials: String,
    val enableRemoteShell: Boolean = true,
    val extraUplinkArgs: Array<String> = arrayOf("-v"),
) : Parcelable {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as UplinkConfig

        if (enableRemoteShell != other.enableRemoteShell) return false
        if (credentials != other.credentials) return false
        if (!extraUplinkArgs.contentEquals(other.extraUplinkArgs)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enableRemoteShell.hashCode()
        result = 31 * result + credentials.hashCode()
        result = 31 * result + extraUplinkArgs.contentHashCode()
        return result
    }
}

@Parcelize
data class UplinkPayload(
    val stream: String,
    val sequence: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val fields: Map<String, @RawValue Any> = HashMap()
) : Parcelable {
    fun toFlatJson(): String {
        val jsonObject = JSONObject()

        jsonObject.put("stream", stream)
        jsonObject.put("sequence", sequence)
        jsonObject.put("timestamp", timestamp)

        for ((key, value) in fields) {
            when (value) {
                is Int, is Float, is Double, is String, is Boolean -> jsonObject.put(key, value)
                else -> throw IllegalArgumentException("Unsupported value type for key '$key': ${value::class.simpleName}")
            }
        }

        return jsonObject.toString()
    }
}

