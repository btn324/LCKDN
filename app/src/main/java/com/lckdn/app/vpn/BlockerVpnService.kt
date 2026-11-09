package com.lckdn.app.vpn

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import androidx.core.content.ContextCompat
import com.lckdn.app.Notifications
import com.lckdn.app.Prefs
import com.lckdn.app.filter.Blocklist
import com.lckdn.app.ui.MainActivity
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Local "DNS only" VPN. Only traffic to a virtual DNS address is routed into the tunnel,
 * so normal browsing speed is untouched. Each lookup is checked against [Blocklist], then
 * forwarded to a family-safe resolver that also blocks adult sites and ads and enforces
 * SafeSearch / YouTube Restricted Mode.
 */
class BlockerVpnService : VpnService() {

    companion object {
        private const val TAG = "LCKDN"
        const val ACTION_START = "com.lckdn.app.action.START"
        const val ACTION_STOP = "com.lckdn.app.action.STOP"

        private const val TUN_ADDRESS = "10.111.222.1"
        private const val DNS_ADDRESS = "10.111.222.2"

        /** AdGuard Family: adult content + ads/trackers + SafeSearch. */
        private val ADGUARD_FAMILY = listOf("94.140.14.15", "94.140.15.16")

        /** Cloudflare for Families: adult content + malware. */
        private val CLOUDFLARE_FAMILY = listOf("1.1.1.3", "1.0.0.3")

        @Volatile
        var running = false
            private set

        fun start(c: Context) {
            try {
                ContextCompat.startForegroundService(
                    c, Intent(c, BlockerVpnService::class.java).setAction(ACTION_START)
                )
            } catch (e: Exception) {
                Log.w(TAG, "Could not start filter", e)
            }
        }

        fun stop(c: Context) {
            try {
                c.startService(Intent(c, BlockerVpnService::class.java).setAction(ACTION_STOP))
            } catch (e: Exception) {
                Log.w(TAG, "Could not stop filter", e)
            }
        }
    }

    private var tun: ParcelFileDescriptor? = null
    private var output: FileOutputStream? = null
    private var worker: Thread? = null
    private val writeLock = Any()
    private var pool: ExecutorService = Executors.newFixedThreadPool(8)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            shutdown()
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        goForeground()
        if (!Prefs.isLocked(this)) {
            shutdown()
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }
        establish()
        return START_STICKY
    }

    private fun goForeground() {
        val n = Notifications.status(this)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(Notifications.ID_STATUS, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(Notifications.ID_STATUS, n)
        }
    }

    @Synchronized
    private fun establish() {
        if (tun != null && running) return
        val configure = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        val builder = Builder()
            .setSession("LCKDN")
            .addAddress(TUN_ADDRESS, 24)
            .addDnsServer(DNS_ADDRESS)
            .addRoute(DNS_ADDRESS, 32)
            .setBlocking(true)
            .setConfigureIntent(configure)
        try {
            builder.addDisallowedApplication(packageName)
        } catch (_: Exception) {
        }
        if (Build.VERSION.SDK_INT >= 29) builder.setMetered(false)

        val iface = try {
            builder.establish()
        } catch (e: Exception) {
            Log.e(TAG, "establish failed", e)
            null
        }
        if (iface == null) {
            // Permission revoked: let the app / notification walk the user through restoring it.
            Notifications.interrupted(this)
            stopForeground(Service.STOP_FOREGROUND_REMOVE)
            stopSelf()
            return
        }
        if (pool.isShutdown) pool = Executors.newFixedThreadPool(8)
        tun = iface
        output = FileOutputStream(iface.fileDescriptor)
        running = true
        Notifications.clearInterrupted(this)
        worker = Thread({ readLoop(iface) }, "lckdn-dns").also { it.start() }
    }

    private fun readLoop(iface: ParcelFileDescriptor) {
        val input = FileInputStream(iface.fileDescriptor)
        val buf = ByteArray(32767)
        try {
            while (running) {
                val n = input.read(buf)
                if (n <= 0) continue
                val pkt = buf.copyOf(n)
                try {
                    handle(pkt)
                } catch (e: Exception) {
                    Log.w(TAG, "packet error", e)
                }
            }
        } catch (_: IOException) {
            // Interface closed.
        } finally {
            running = false
        }
    }

    private fun handle(pkt: ByteArray) {
        if (pkt.size < 28) return
        if (((pkt[0].toInt() shr 4) and 0xF) != 4) return
        val ihl = (pkt[0].toInt() and 0xF) * 4
        if (pkt[9].toInt() != 17) return // UDP only
        if (pkt.size < ihl + 8 + 12) return
        if (Packets.u16(pkt, ihl + 2) != 53) return

        val udpLen = Packets.u16(pkt, ihl + 4)
        val end = minOf(pkt.size, ihl + udpLen)
        if (end <= ihl + 8) return
        val query = pkt.copyOfRange(ihl + 8, end)
        val name = Packets.dnsQuestionName(query) ?: return

        if (Blocklist.isBlockedDomain(name, Prefs.flag(this, Prefs.BLOCK_ADS))) {
            val resp = Packets.dnsBlockedResponse(query) ?: return
            write(Packets.udpReply(pkt, ihl, resp))
            return
        }
        try {
            pool.execute {
                val resp = forward(query) ?: return@execute
                write(Packets.udpReply(pkt, ihl, resp))
            }
        } catch (_: Exception) {
            // Pool shut down during stop.
        }
    }

    private fun upstreams(): List<String> =
        if (Prefs.flag(this, Prefs.BLOCK_ADS)) ADGUARD_FAMILY + CLOUDFLARE_FAMILY
        else CLOUDFLARE_FAMILY + ADGUARD_FAMILY

    private fun forward(query: ByteArray): ByteArray? {
        for (server in upstreams()) {
            try {
                DatagramSocket().use { socket ->
                    protect(socket)
                    socket.soTimeout = 2500
                    socket.send(DatagramPacket(query, query.size, InetAddress.getByName(server), 53))
                    val rb = ByteArray(4096)
                    val rp = DatagramPacket(rb, rb.size)
                    socket.receive(rp)
                    return rb.copyOf(rp.length)
                }
            } catch (_: Exception) {
                // Try next resolver.
            }
        }
        return null
    }

    private fun write(packet: ByteArray) {
        synchronized(writeLock) {
            try {
                output?.write(packet)
            } catch (_: IOException) {
            }
        }
    }

    @Synchronized
    private fun shutdown() {
        running = false
        try {
            tun?.close()
        } catch (_: IOException) {
        }
        tun = null
        output = null
        worker?.interrupt()
        worker = null
        pool.shutdownNow()
    }

    override fun onRevoke() {
        shutdown()
        if (Prefs.isLocked(this)) Notifications.interrupted(this)
        stopForeground(Service.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        shutdown()
        super.onDestroy()
    }
}
