package com.egor201.puffy.core

// VpnService — owns the tun interface and foreground notification
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import androidx.core.app.NotificationCompat
import com.egor201.puffy.MainActivity
import com.egor201.puffy.R
import com.egor201.puffy.model.ServerProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class VpnCoreService : VpnService() {

    companion object {
        const val ACTION_CONNECT = "com.egor201.puffy.CONNECT"
        const val ACTION_DISCONNECT = "com.egor201.puffy.DISCONNECT"
        const val EXTRA_CONFIG_JSON = "config_json"
        const val EXTRA_PROFILE_NAME = "profile_name"

        private const val CHANNEL_ID = "puffy_vpn_channel"
        private const val NOTIFICATION_ID = 1

        var isConnected: Boolean = false
            private set
    }

    private var tunInterface: ParcelFileDescriptor? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CONNECT -> {
                val config = intent.getStringExtra(EXTRA_CONFIG_JSON) ?: return START_NOT_STICKY
                val name = intent.getStringExtra(EXTRA_PROFILE_NAME) ?: "Puffy"
                connect(config, name)
            }
            ACTION_DISCONNECT -> disconnect()
        }
        return START_STICKY
    }

    private fun connect(configJson: String, profileName: String) {
        scope.launch {
            val fd = establishTun()
            if (fd == null) {
                isConnected = false
                return@launch
            }
            tunInterface = fd

            val started = XrayEngine.start(applicationContext, configJson, fd.fd)
            isConnected = started
            if (started) {
                startForeground(NOTIFICATION_ID, buildNotification(profileName))
            } else {
                disconnect()
            }
        }
    }

    private fun establishTun(): ParcelFileDescriptor? {
        val builder = Builder()
            .setSession("Puffy")
            .setMtu(1500)
            .addAddress("10.10.14.1", 30)
            .addDnsServer("1.1.1.1")
            .addDnsServer("8.8.8.8")
            .addRoute("0.0.0.0", 0)

        return try {
            builder.establish()
        } catch (e: Exception) {
            null
        }
    }

    private fun disconnect() {
        XrayEngine.stop()
        isConnected = false
        try {
            tunInterface?.close()
        } catch (e: Exception) {
        }
        tunInterface = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun buildNotification(profileName: String): Notification {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            manager.createNotificationChannel(channel)
        }

        val openAppIntent = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentTitle(getString(R.string.notification_connected, profileName))
            .setContentIntent(openAppIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    override fun onRevoke() {
        disconnect()
        super.onRevoke()
    }
}

fun buildConnectIntent(context: android.content.Context, profile: ServerProfile): Intent {
    val configJson = XrayConfigBuilder.build(profile)
    return Intent(context, VpnCoreService::class.java).apply {
        action = VpnCoreService.ACTION_CONNECT
        putExtra(VpnCoreService.EXTRA_CONFIG_JSON, configJson)
        putExtra(VpnCoreService.EXTRA_PROFILE_NAME, profile.name)
    }
}

fun buildDisconnectIntent(context: android.content.Context): Intent {
    return Intent(context, VpnCoreService::class.java).apply {
        action = VpnCoreService.ACTION_DISCONNECT
    }
}
