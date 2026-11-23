package studio.wingart.ctrl

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/** One connection (Wi-Fi TCP or Bluetooth RFCOMM) speaking newline-delimited JSON to the Windows app. */
object Link {
    val BT_UUID: UUID = UUID.fromString("7c2f6e3a-5b0d-4c1e-9a58-3f9d2b6a1e47")
    const val PORT = 8765
    const val DISC_PORT = 8766
    var connected by mutableStateOf(false)
    var busy by mutableStateOf(false)
    var gamepadOk by mutableStateOf(true)
    var hostName by mutableStateOf("")
    var lastError by mutableStateOf("")
    @Volatile private var out: OutputStream? = null
    @Volatile private var closer: Closeable? = null
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    // Fast streams (mouse move, scroll, gamepad state) are coalesced: if the network is busy we send ONE
    // up-to-date packet instead of queueing hundreds of old ones. This is what stops the "freeze after a while".
    private val mvX = AtomicInteger(0)
    private val mvY = AtomicInteger(0)
    private val mvBusy = AtomicBoolean(false)
    private val scY = AtomicInteger(0)
    private val scBusy = AtomicBoolean(false)
    private val gsBusy = AtomicBoolean(false)

    private fun write(o: JSONObject) {
        val s = out ?: return
        try { s.write((o.toString() + "\n").toByteArray()); s.flush() } catch (e: Exception) { close() }
    }

    fun send(o: JSONObject) { if (out != null) io.execute { write(o) } }
    fun send(vararg p: Pair<String, Any>): Unit = send(JSONObject(mapOf(*p)))

    fun move(dx: Int, dy: Int) {
        if (out == null) return
        mvX.addAndGet(dx); mvY.addAndGet(dy)
        if (mvBusy.compareAndSet(false, true)) io.execute {
            mvBusy.set(false)
            val x = mvX.getAndSet(0); val y = mvY.getAndSet(0)
            if (x != 0 || y != 0) write(JSONObject().put("t", "mv").put("x", x).put("y", y))
        }
    }

    fun scroll(dy: Int) {
        if (out == null) return
        scY.addAndGet(dy)
        if (scBusy.compareAndSet(false, true)) io.execute {
            scBusy.set(false)
            val y = scY.getAndSet(0)
            if (y != 0) write(JSONObject().put("t", "sc").put("y", y))
        }
    }

    fun pushPad() {
        if (out == null) return
        if (gsBusy.compareAndSet(false, true)) io.execute { gsBusy.set(false); write(Pad.state()) }
    }

    fun close() {
        out = null
        try { closer?.close() } catch (_: Exception) {}
        closer = null
        Pad.clear()
        main.post { connected = false; busy = false }
    }

    /** result callback: null = success, otherwise "pin" or "conn". */
    fun connectWifi(host: String, pin: String, cb: (String?) -> Unit) {
        busy = true
        thread {
            try {
                val s = Socket(); s.tcpNoDelay = true; s.keepAlive = true
                s.connect(InetSocketAddress(host, PORT), 4000)
                finish(s.getInputStream(), s.getOutputStream(), s, pin, cb)
            } catch (e: Exception) { main.post { lastError = e.message ?: ""; busy = false; cb("conn") } }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBt(dev: BluetoothDevice, pin: String, cb: (String?) -> Unit) {
        busy = true
        thread {
            var sock: BluetoothSocket? = null
            var err: Exception? = null
            try { BluetoothAdapter.getDefaultAdapter()?.cancelDiscovery() } catch (_: Exception) {}
            // secure first, then insecure: some phone/PC combinations only accept one of them
            for (insecure in booleanArrayOf(false, true)) {
                try {
                    val s = if (insecure) dev.createInsecureRfcommSocketToServiceRecord(BT_UUID) else dev.createRfcommSocketToServiceRecord(BT_UUID)
                    s.connect(); sock = s; break
                } catch (e: Exception) { err = e }
            }
            val s = sock
            if (s == null) { val m = err?.message ?: ""; main.post { lastError = m; busy = false; cb("conn") }; return@thread }
            finish(s.inputStream, s.outputStream, s, pin, cb)
        }
    }

    private fun finish(i: InputStream, o: OutputStream, c: Closeable, pin: String, cb: (String?) -> Unit) {
        try {
            o.write((JSONObject().put("t", "auth").put("pin", pin).put("hb", 1).toString() + "\n").toByteArray()); o.flush()
            val rd = i.bufferedReader()
            val reply = JSONObject(rd.readLine() ?: throw Exception("closed"))
            if (!reply.optBoolean("hello")) { c.close(); main.post { busy = false; cb("pin") }; return }
            out = o; closer = c
            main.post { lastError = ""; gamepadOk = reply.optBoolean("gamepad"); hostName = reply.optString("name"); connected = true; busy = false; cb(null) }
            // reader: detects the PC closing the link
            thread { try { while (rd.readLine() != null) { } } catch (_: Exception) {}; if (out === o) close() }
            // heartbeat: lets the PC notice a dead phone, and lets us notice a dead PC
            thread { while (out === o) { try { Thread.sleep(3000) } catch (_: Exception) {}; if (out === o) send(JSONObject().put("t", "p")) } }
        } catch (e: Exception) { try { c.close() } catch (_: Exception) {}; main.post { lastError = e.message ?: ""; busy = false; cb("conn") } }
    }

    /** Finds laptops on the local Wi-Fi via UDP broadcast (global + every subnet broadcast). Returns (name, ip). */
    fun discover(cb: (List<Pair<String, String>>) -> Unit) = thread {
        val found = LinkedHashMap<String, String>()
        try {
            val targets = LinkedHashSet<InetAddress>()
            targets.add(InetAddress.getByName("255.255.255.255"))
            try {
                for (ni in NetworkInterface.getNetworkInterfaces()) {
                    if (!ni.isUp || ni.isLoopback) continue
                    for (ia in ni.interfaceAddresses) { val b = ia.broadcast; if (b != null) targets.add(b) }
                }
            } catch (_: Exception) {}
            DatagramSocket().use { s ->
                s.broadcast = true; s.soTimeout = 1200
                val msg = "WINGART?".toByteArray()
                for (t in targets) { try { s.send(DatagramPacket(msg, msg.size, t, DISC_PORT)) } catch (_: Exception) {} }
                val buf = ByteArray(256)
                while (true) {
                    val p = DatagramPacket(buf, buf.size); s.receive(p)
                    val parts = String(p.data, 0, p.length).split("|")
                    if (parts.size >= 3 && parts[0] == "WINGART") found[p.address.hostAddress ?: ""] = parts[1]
                }
            }
        } catch (_: Exception) {}
        main.post { cb(found.map { it.value to it.key }) }
    }
}

/** Gamepad state, pushed as one packet (coalesced by Link). */
object Pad {
    @Volatile var lx = 0f
    @Volatile var ly = 0f
    @Volatile var rx = 0f
    @Volatile var ry = 0f
    @Volatile var lt = 0f
    @Volatile var rt = 0f

    fun state(): JSONObject = JSONObject().put("t", "gs")
        .put("lx", lx.toDouble()).put("ly", ly.toDouble()).put("rx", rx.toDouble()).put("ry", ry.toDouble())
        .put("lt", lt.toDouble()).put("rt", rt.toDouble())

    fun push() = Link.pushPad()

    fun btn(k: String, down: Boolean) {
        if (k == "lt") { lt = if (down) 1f else 0f; push() }
        else if (k == "rt") { rt = if (down) 1f else 0f; push() }
        else Link.send("t" to "gp", "b" to k, "s" to (if (down) "d" else "u"))
    }

    fun clear() { lx = 0f; ly = 0f; rx = 0f; ry = 0f; lt = 0f; rt = 0f }

    /** Zero everything here AND tell the PC to release every key / mouse button / gamepad button. */
    fun release() { clear(); Link.send("t" to "reset"); push() }
}
