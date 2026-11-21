package studio.wingart.ctrl

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.json.JSONObject
import java.io.Closeable
import java.io.OutputStream
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.util.UUID
import java.util.concurrent.Executors
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
    @Volatile private var out: OutputStream? = null
    @Volatile private var closer: Closeable? = null
    private val io = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())

    fun send(o: JSONObject) {
        val s = out ?: return
        io.execute { try { s.write((o.toString() + "\n").toByteArray()); s.flush() } catch (e: Exception) { close() } }
    }
    fun send(vararg p: Pair<String, Any>): Unit = send(JSONObject(mapOf(*p)))

    fun close() {
        out = null
        try { closer?.close() } catch (_: Exception) {}
        closer = null
        main.post { connected = false; busy = false }
    }

    /** result callback: null = success, otherwise "pin" or "conn". */
    fun connectWifi(host: String, pin: String, cb: (String?) -> Unit) {
        busy = true
        thread {
            try {
                val s = Socket(); s.tcpNoDelay = true
                s.connect(InetSocketAddress(host, PORT), 4000)
                finish(s.getInputStream(), s.getOutputStream(), s, pin, cb)
            } catch (e: Exception) { main.post { busy = false; cb("conn") } }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectBt(dev: BluetoothDevice, pin: String, cb: (String?) -> Unit) {
        busy = true
        thread {
            try {
                val s = dev.createRfcommSocketToServiceRecord(BT_UUID)
                s.connect()
                finish(s.inputStream, s.outputStream, s, pin, cb)
            } catch (e: Exception) { main.post { busy = false; cb("conn") } }
        }
    }

    private fun finish(i: java.io.InputStream, o: OutputStream, c: Closeable, pin: String, cb: (String?) -> Unit) {
        try {
            o.write((JSONObject(mapOf("t" to "auth", "pin" to pin)).toString() + "\n").toByteArray()); o.flush()
            val rd = i.bufferedReader()
            val reply = JSONObject(rd.readLine() ?: throw Exception("closed"))
            if (!reply.optBoolean("hello")) { c.close(); main.post { busy = false; cb("pin") }; return }
            out = o; closer = c
            main.post { gamepadOk = reply.optBoolean("gamepad"); hostName = reply.optString("name"); connected = true; busy = false; cb(null) }
            thread { try { while (rd.readLine() != null) { } } catch (_: Exception) {}; close() }
        } catch (e: Exception) { try { c.close() } catch (_: Exception) {}; main.post { busy = false; cb("conn") } }
    }

    /** Finds laptops on the local Wi-Fi via UDP broadcast. Returns (name, ip). */
    fun discover(cb: (List<Pair<String, String>>) -> Unit) = thread {
        val found = LinkedHashMap<String, String>()
        try {
            DatagramSocket().use { s ->
                s.broadcast = true; s.soTimeout = 1200
                val msg = "WINGART?".toByteArray()
                s.send(DatagramPacket(msg, msg.size, InetAddress.getByName("255.255.255.255"), DISC_PORT))
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

/** Gamepad state, pushed as one packet. */
object Pad {
    var lx = 0f; var ly = 0f; var rx = 0f; var ry = 0f; var lt = 0f; var rt = 0f
    fun push() = Link.send("t" to "gs", "lx" to lx, "ly" to ly, "rx" to rx, "ry" to ry, "lt" to lt, "rt" to rt)
    fun btn(k: String, down: Boolean) {
        if (k == "lt") { lt = if (down) 1f else 0f; push() }
        else if (k == "rt") { rt = if (down) 1f else 0f; push() }
        else Link.send("t" to "gp", "b" to k, "s" to if (down) "d" else "u")
    }
}
