package io.bytebeam.exampleapp

import android.util.Log
import UplinkConfig
import android.Manifest.permission.POST_NOTIFICATIONS
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.bytebeam.uplink_android.startUplinkService
import io.bytebeam.uplink_android.stopUplinkService

// language=json
val testDeviceJson = """
    {
      "project_id": "sagar",
      "broker": "cloud.bytebeam.io",
      "port": 8883,
      "device_id": "1",
      "authentication": {
        "ca_certificate": "-----BEGIN CERTIFICATE-----\nMIIFrDCCA5SgAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\ndGViZWFtLmlvMB4XDTIxMDkwMjExMDYyM1oXDTMxMDkwMjExMDYyM1owdzEOMAwG\nA1UEBhMFSW5kaWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxv\ncmUxFzAVBgNVBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDAS\nBgNVBAoTC0J5dGViZWFtLmlvMIICIjANBgkqhkiG9w0BAQEFAAOCAg8AMIICCgKC\nAgEAr/bnOa/8AUGZmd/s+7rejuROgeLqqU9X15KKfKOBqcoMyXsSO65UEwpzadpw\nMl7GDCdHqFTymqdnAnbhgaT1PoIFhOG64y7UiNgiWmbh0XJj8G6oLrW9rQ1gug1Q\n/D7x2fUnza71aixiwEL+KsIFYIdDuzmoRD3rSer/bKOcGGs0WfB54KqIVVZ1DwsU\nk1wx5ExsKo7gAdXMAbdHRI2Szmn5MsZwGL6V0LfsKLE8ms2qlZe50oo2woLNN6XP\nRfRL4bwwkdsCqXWkkt4eUSNDq9hJsuINHdhO3GUieLsKLJGWJ0lq6si74t75rIKb\nvvsFEQ9mnAVS+iuUUsSjHPJIMnn/J64Nmgl/R/8FP5TUgUrHvHXKQkJ9h/a7+3tS\nlV2KMsFksXaFrGEByGIJ7yR4qu9hx5MXf8pf8EGEwOW/H3CdWcC2MvJ11PVpceUJ\neDVwE7B4gPM9Kx02RNwvUMH2FmYqkXX2DrrHQGQuq+6VRoN3rEdmGPqnONJEPeOw\nZzcGDVXKWZtd7UCbcZKdn0RYmVtI/OB5OW8IRoXFYgGB3IWP796dsXIwbJSqRb9m\nylICGOceQy3VR+8+BHkQLj5/ZKTe+AA3Ktk9UADvxRiWKGcejSA/LvyT8qzz0dqn\nGtcHYJuhJ/XpkHtB0PykB5WtxFjx3G/osbZfrNflcQZ9h1MCAwEAAaNCMEAwDgYD\nVR0PAQH/BAQDAgKEMA8GA1UdEwEB/wQFMAMBAf8wHQYDVR0OBBYEFKl/MTbLrZ0g\nurneOmAfBHO+LHz+MA0GCSqGSIb3DQEBCwUAA4ICAQAlus/uKic5sgo1d2hBJ0Ak\ns1XJsA2jz+OEdshQHmCCmzFir3IRSuVRmDBaBGlJDHCELqYxKn6dl/sKGwoqoAQ5\nOeR2sey3Nmdyw2k2JTDx58HnApZKAVir7BDxbIbbHmfhJk4ljeUBbertNXWbRHVr\ncs4XBNwXvX+noZjQzmXXK89YBsV2DCrGRAUeZ4hQEqV7XC0VKmlzEmfkr1nibDr5\nqwbI+7QWIAnkHggYi27lL2UTHpbsy9AnlrRMe73upiuLO7TvkwYC4TyDaoQ2ZRpG\nHY+mxXLdftoMv/ZvmyjOPYeTRQbfPqoRqcM6XOPXwSw9B6YddwmnkI7ohNOvAVfD\nwGptUc5OodgFQc3waRljX1q2lawZCTh58IUf32CRtOEL2RIz4VpUrNF/0E2vts1f\npO7V1vY2Qin998Nwqkxdsll0GLtEEE9hUyvk1F8U+fgjJ3Rjn4BxnCN4oCrdJOMa\nJCaysaHV7EEIMqrYP4jH6RzQzOXLd0m9NaL8A/Y9z2a96fwpZZU/fEEOH71t3Eo3\nV/CKlysiALMtsHfZDwHNpa6g0NQNGN5IRl/w1TS1izzjzgWhR6r8wX8OPLRzhNRz\n2HDbTXGYsem0ihC0B8uzujOhTHcBwsfxZUMpGjg8iycJlfpPDWBdw8qrGu8LeNux\na0cIevjvYAtVysoXInV0kg==\n-----END CERTIFICATE-----\n",
        "device_certificate": "-----BEGIN CERTIFICATE-----\nMIIEZTCCAk2gAwIBAgICB+MwDQYJKoZIhvcNAQELBQAwdzEOMAwGA1UEBhMFSW5k\naWExETAPBgNVBAgTCEthcm5hdGFrMRIwEAYDVQQHEwlCYW5nYWxvcmUxFzAVBgNV\nBAkTDlN1YmJpYWggR2FyZGVuMQ8wDQYDVQQREwY1NjAwMTExFDASBgNVBAoTC0J5\ndGViZWFtLmlvMB4XDTI0MTIxMzA2MDkwOVoXDTM0MTIxMzA2MDkwOVowHDEOMAwG\nA1UEChMFc2FnYXIxCjAIBgNVBAMTATEwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAw\nggEKAoIBAQDxM9W9OpdQIxsx9eTDw0OxEZQR0WNYqoiewy+XxONfUn4gTDgqgWdZ\nir3ZIPS3GYigV4OhWovP+dMpuTLjtDjrGixmnbx9PzNCyEGKG6D0Uca45ZVeR4yG\nMbSWdUOFxpxyC8llI8cCe0WU6kUF53wIhvDWsbJ8lTGXqeUiUdkVOivt+2Jx03Kp\nrkzQh7vlkzLmgy6qkFM5xKDNcViTz9Io3RYkfzi7JVm31DX263dibsBo5FNqynav\nXnb79a370CHX0GSbZlzYH5P61jF/0QbmRjB/pKFNa18CjtvcQ0gpyvrbPTZkH8/4\n6a/5cM4jN9lDBwnKBEnKvkv7MpkuJqNDAgMBAAGjVjBUMA4GA1UdDwEB/wQEAwIF\noDATBgNVHSUEDDAKBggrBgEFBQcDAjAfBgNVHSMEGDAWgBSpfzE2y62dILq53jpg\nHwRzvix8/jAMBgNVHREEBTADggExMA0GCSqGSIb3DQEBCwUAA4ICAQCKSkOxe+W8\nb1bsOQ5bZtnyvyIYEPJQ1D1V7/E56/55+cH4snHUe7qatsr+MWA5aS0Orcxbt6kI\n+YXbrnjmx4z33rAtFIShJEOXrs+4+B/6KxjSr/AiWR63e98uKhAp/bNrwD39MwTH\nrZumo4/ZyTDp+YjeGeAIDd2l0bETiOo/x34mT0QJ0btkP7AG/TCLu8OkCWl1DXjo\n0UYdCuEVUGfZS1qIkiVr7deYJah+Z9Atvh+MTYUAsPSjW32p+AWrBBieK645cmxU\nZDsOIZ4SajRQlUe8B3yEz8K+Bkh2gieMKwcHNQ8duF/UPwZIJ9JDVZ9XNbx4r/R2\nUQMK1h7rrC7FN+u7PLwbh1VsZr8fmqTJGvW8ivFrnnTVdQZgIrNanHW88ZZzNzM3\n2fK97/3vNWa+m77K9YnjgSBsdOSLz0uDEilARXHSvCdjrAgYHUPFe83x4eRGeGec\ng+l8ZmigbPntezq3wt+O24Lg2WaW2GX2qkwEZN4n0rrdWCNxk6tvLNauIanZKsGY\nKUawyLZx3WXhlbBKLM25jcKAY58/biDncwuyfQs+27EF19jJWPEcryJ+yK1C+vdR\na0x8ZeEzaQHzHWZ6bf5umfxnzFnyPwMPV9PgF2FREYyEq9L3h3MGVg4VWbu8bvfS\njuGA2clbLYU8Y457u0r05wjz/m/mWijCcQ==\n-----END CERTIFICATE-----\n",
        "device_private_key": "-----BEGIN RSA PRIVATE KEY-----\nMIIEpAIBAAKCAQEA8TPVvTqXUCMbMfXkw8NDsRGUEdFjWKqInsMvl8TjX1J+IEw4\nKoFnWYq92SD0txmIoFeDoVqLz/nTKbky47Q46xosZp28fT8zQshBihug9FHGuOWV\nXkeMhjG0lnVDhcaccgvJZSPHAntFlOpFBed8CIbw1rGyfJUxl6nlIlHZFTor7fti\ncdNyqa5M0Ie75ZMy5oMuqpBTOcSgzXFYk8/SKN0WJH84uyVZt9Q19ut3Ym7AaORT\nasp2r152+/Wt+9Ah19Bkm2Zc2B+T+tYxf9EG5kYwf6ShTWtfAo7b3ENIKcr62z02\nZB/P+Omv+XDOIzfZQwcJygRJyr5L+zKZLiajQwIDAQABAoIBAQDIWKnc9+HfaomY\nQgq2oGaFmXhIJIhEicHZPX82rQ+/iaHB6g1tIPdbZOa0h688tzTJGmf3imI/ad9/\n9JjUkkoE3kSrJ7H8xpYpjnUB5lkXFyXdRrV/Dicm//pkTxq673HbOPcbz0/qO8Jc\nIwsaEGDyJEO/0PHUkYgT1cbBbwLFoWMI6Mk3xWUQ5O9djHEPIF57kFPlPRdz8jkC\nTuArvxiGNky6OgUPhyAx83js0VBanXEKZ+rzxZRduPDYZBsIHjv5W0YHMbeStup6\nP+a7964wy3Re5W6Ee3If9GOv3Ti/WA86gz7M80c05clph78K82aYroUzdlmonW9s\nwl1wYtVBAoGBAP/KyVjrg8tgFPakx1tQqVfagXDd4uNrE0PhohuJhSG+L1EA4V6S\nFq0bsj4zKf7ba8LdeRHqVCW1hkr19734UHwqmU4m/aHxLiTlX0psRarjKyzsmJZ/\nQ31OuQ8jBz+6ETCsUsve2mMjKJt5sT3jSOKSDLpUr1GqcWWjklXlprzJAoGBAPFm\nA2UAFPCtdSSSvjh1ri3o96R0IQvgx/z7bFsRtO7OkjeCDdtRw3zX70b6DYS8cg16\ncmXPA8Zl452npl/rc71EhCjb8k0jLuv3EskSwwLiC/Wpdhx6jNQWCTZKSFBPl7X3\n00DkCBmrnNueFcpwRDVLUUPhVBRxFFWsvN04W8GrAoGBAJuBCrs2Ip9nQBdZwaCv\n/uNcAUk+e4rKM6IW79hR2E/VMSrLoDdAFO0UY14Q+LzpZC/JOKs9i/6IxqWXtw8U\ngMmblCvA5HypBOaFU1MJU6k6BauAApurrrnlO/gJ0YRad8zhVkx+pMGUREGQz8HS\nBSNIqtg0V4kMV3f04ye5P46RAoGAYrf2MWqsJZS58B/2nH47odjA1UcMcKAXCPUE\n5eVC2douX3bXFdbFHBvuZVdDCgJKngpyGAJlp8/pGoMB/f360e7gIRl5aGQ3/xWG\nYCZFC7vHakpH2/Od/emZENOl6Pnzr93OTZ4zVdQhjIqEnMn2itjnPdw7FzT7POIs\nRkeS9osCgYBoCgLGDVex6eFJmsrs5D4Remd6A8u/PqRiiRO3T4xYJYKqPyhqAdAn\nUJ6V1N8pW2c/dFznHXHY+GP77kgVIxsbbhVocIXvc4XTcfMJOxDTQjxIdis8luH7\nRTewBNnK3TRkZa1hYZMusmJlZQoYRVAYgbDUZsg1q0V7FC7NevzN/w==\n-----END RSA PRIVATE KEY-----\n"
      }
    }
""".trimIndent()

val uplinkConfig = UplinkConfig(testDeviceJson, true, extraUplinkArgs = arrayOf("-v"))

class MainActivity : AppCompatActivity() {
    var idx: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
            && ContextCompat.checkSelfPermission(this, POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            val requestPermissionLauncher = this.registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) { isGranted: Boolean ->
                if (isGranted) {
                    doSetup()
                }
            }

            requestPermissionLauncher.launch(POST_NOTIFICATIONS)
          } else {
              doSetup()
          }
    }

    fun doSetup() {
        startUplinkService(this, "monitoring service is running", uplinkConfig)

        findViewById<Button>(R.id.start_btn).setOnClickListener {
            startUplinkService(this, "monitoring service is running", uplinkConfig)
        }
        findViewById<Button>(R.id.stop_btn).setOnClickListener {
            stopUplinkService(this)
        }
    }
}