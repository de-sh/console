import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UplinkConfig(
    val credentials: String,
    val enableRemoteShell: Boolean = true,
    val uplinkLogLevel: Int = 1
) : Parcelable

