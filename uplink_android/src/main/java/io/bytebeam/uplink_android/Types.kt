import android.os.Parcelable
import kotlinx.parcelize.Parcelize


@Parcelize
data class DeviceCredentials(val id: String) : Parcelable

@Parcelize
data class UplinkConfig(val credentials: String) : Parcelable

