package com.egor201.puffy.core

// Native Xray core bridge (libv2ray.aar)
// FUTURE: this is the intended FFI boundary for a Rust rewrite —
// everything above this file (UI, parsing, config, repository) stays
// in Kotlin either way, since Activity/VpnService can't leave the JVM.
import android.content.Context
import libv2ray.CoreCallbackHandler
import libv2ray.CoreController
import libv2ray.Libv2ray

object XrayEngine {

    private var initialized = false
    private var controller: CoreController? = null

    @Synchronized
    fun ensureInit(context: Context) {
        if (initialized) return
        Libv2ray.initCoreEnv(context.filesDir.absolutePath, "")
        initialized = true
    }

    @Synchronized
    private fun getController(): CoreController {
        val existing = controller
        if (existing != null) return existing

        val created = Libv2ray.newCoreController(object : CoreCallbackHandler {
            override fun startup(): Long = 0
            override fun shutdown(): Long = 0
            override fun onEmitStatus(code: Long, message: String?): Long = 0
        })
        controller = created
        return created
    }

    fun start(context: Context, configJson: String, tunFd: Int): Boolean {
        ensureInit(context)
        return try {
            getController().startLoop(configJson, tunFd)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stop() {
        try {
            controller?.stopLoop()
        } catch (e: Exception) {
        }
    }

    fun isRunning(): Boolean = try {
        controller?.isRunning ?: false
    } catch (e: Exception) {
        false
    }
}
