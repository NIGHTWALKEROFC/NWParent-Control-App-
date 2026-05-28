// PATH: app/src/main/java/com/nw/parentalcontrol/data/Models.kt
package com.nw.parentalcontrol.data

data class ChildDevice(
    val deviceId: String        = "",
    val deviceName: String      = "",
    val connectedAt: Long       = 0L,
    val parentDeviceId: String  = "",
    val fcmToken: String        = "",
    val permissions: ChildPermissions = ChildPermissions(),
    val isOnline: Boolean       = false,
    val isConnected: Boolean    = false,
    val lastSeen: Long          = 0L
)

data class ChildPermissions(
    val camera: Boolean        = false,
    val microphone: Boolean    = false,
    val screenShare: Boolean   = false,
    val storage: Boolean       = false,
    val notifications: Boolean = false,
    val contacts: Boolean      = false,
    val accessibility: Boolean = false,
    val usageStats: Boolean    = false,
    val callLog: Boolean       = false,
    val sms: Boolean           = false,
    val location: Boolean      = false,
    val overlay: Boolean       = false
)

data class ControlCommand(
    val type: String        = "",
    val value: String       = "",
    val timestamp: Long     = System.currentTimeMillis(),
    val fromParent: Boolean = true,
    val expiresAt: Long     = System.currentTimeMillis() + 60_000L
)

data class UpdateInfo(
    val versionCode: Int    = 0,
    val versionName: String = "",
    val downloadUrl: String = "",
    val releaseNotes: String = "",
    val mandatory: Boolean  = false
)

data class LocationData(
    val lat: Double     = 0.0,
    val lng: Double     = 0.0,
    val accuracy: Float = 0f,
    val address: String = "",
    val timestamp: Long = 0L
)

data class CallLogEntry(
    val number: String   = "",
    val name: String     = "",
    val type: String     = "",
    val duration: Long   = 0L,
    val timestamp: Long  = 0L
)

data class SmsEntry(
    val number: String   = "",
    val body: String     = "",
    val type: String     = "",
    val timestamp: Long  = 0L
)

data class AppUsageEntry(
    val packageName: String = "",
    val appName: String     = "",
    val usageMinutes: Int   = 0,
    val date: String        = ""
)

data class BrowserHistoryEntry(
    val url: String      = "",
    val pkg: String      = "",
    val timestamp: Long  = 0L
)

object CommandTypes {
    const val ENABLE_CAMERA      = "enable_camera"
    const val DISABLE_CAMERA     = "disable_camera"
    const val ENABLE_MIC         = "enable_mic"
    const val DISABLE_MIC        = "disable_mic"
    const val START_SCREEN_SHARE = "start_screen_share"
    const val STOP_SCREEN_SHARE  = "stop_screen_share"
    const val BLOCK_APP          = "block_app"
    const val UNBLOCK_APP        = "unblock_app"
    const val SET_APP_LIMIT      = "set_app_limit"
    const val APPROVE_DISCONNECT = "approve_disconnect"
    const val DENY_DISCONNECT    = "deny_disconnect"
    const val APPROVE_DELETE     = "approve_delete"
    const val DENY_DELETE        = "deny_delete"
    const val FORCE_DISCONNECT   = "force_disconnect"
    const val LOCK_DEVICE        = "lock_device"
    const val SET_PIN            = "set_pin"
    const val TAKE_SCREENSHOT    = "take_screenshot"
    const val SYNC_CALL_LOG      = "sync_call_log"
    const val SYNC_SMS           = "sync_sms"
}