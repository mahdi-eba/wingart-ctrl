package studio.wingart.ctrl

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.platform.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

const val VERSION = "1.0.0"
const val LINK = "https://t.me/wingartstudio"
val Y = Color(0xFFFCEE09); val BG = Color(0xFF0A0A0A); val PN = Color(0xFF17170A); val DIM = Color(0xFF8C8506); val RED = Color(0xFFFF5A4D)

val STR = mapOf(
    "en" to mapOf("wifi" to "Wi-Fi", "bluetooth" to "Bluetooth", "pin" to "PIN (shown on your PC)", "ip" to "Laptop address", "find" to "Find my laptop",
        "connect" to "Connect", "connecting" to "Connecting…", "disconnect" to "Disconnect", "trackpad" to "Trackpad", "keys" to "Keys", "media" to "Media", "game" to "Game",
        "about" to "About", "version" to "Version", "err_pin" to "Wrong PIN", "err_conn" to "Could not connect", "none" to "No laptop found",
        "bt_hint" to "Pair your laptop in the phone's Bluetooth settings first, then come back.", "bt_perm" to "Bluetooth permission is needed",
        "type" to "Type here…", "pad_hint" to "Move · tap to click · two fingers to scroll", "nopad" to "Gamepad driver missing on PC (ViGEmBus)",
        "about_text" to "Control your laptop from your phone: trackpad, keyboard, media and gamepad.\nBuilt by Wingart Studio.", "tg" to "Telegram channel", "close" to "Close",
        "left" to "Left", "mid" to "Mid", "right" to "Right"),
    "fa" to mapOf("wifi" to "وای‌فای", "bluetooth" to "بلوتوث", "pin" to "کد اتصال (روی PC نمایش داده می‌شود)", "ip" to "آدرس لپ‌تاپ", "find" to "پیدا کردن لپ‌تاپ",
        "connect" to "اتصال", "connecting" to "در حال اتصال…", "disconnect" to "قطع اتصال", "trackpad" to "ترک‌پد", "keys" to "کیبورد", "media" to "مدیا", "game" to "بازی",
        "about" to "درباره ما", "version" to "نسخه", "err_pin" to "کد اشتباه است", "err_conn" to "اتصال برقرار نشد", "none" to "لپ‌تاپی پیدا نشد",
        "bt_hint" to "اول لپ‌تاپ را از تنظیمات بلوتوث گوشی جفت کنید و برگردید.", "bt_perm" to "اجازه‌ی بلوتوث لازم است",
        "type" to "اینجا بنویسید…", "pad_hint" to "حرکت · لمس برای کلیک · دو انگشت برای اسکرول", "nopad" to "درایور دسته روی PC نصب نیست (ViGEmBus)",
        "about_text" to "لپ‌تاپ را با گوشی کنترل کنید: ترک‌پد، کیبورد، مدیا و دسته‌ی بازی.\nساخته‌شده توسط Wingart Studio.", "tg" to "کانال تلگرام", "close" to "بستن",
        "left" to "چپ", "mid" to "وسط", "right" to "راست"))
val LocalLang = compositionLocalOf { "en" }
@Composable fun tr(k: String) = STR[LocalLang.current]?.get(k) ?: k

class Cut(private val c: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(Path().apply {
        moveTo(0f, 0f); lineTo(size.width - c, 0f); lineTo(size.width, c); lineTo(size.width, size.height); lineTo(c, size.height); lineTo(0f, size.height - c); close()
    })
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContent { App() }
    }
}

@Composable fun App() {
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("wc", Context.MODE_PRIVATE) }
    var lang by remember { mutableStateOf(prefs.getString("lang", null) ?: if (java.util.Locale.getDefault().language == "fa") "fa" else "en") }
    CompositionLocalProvider(LocalLang provides lang, LocalLayoutDirection provides if (lang == "fa") LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Box(Modifier.fillMaxSize().background(BG).systemBarsPadding()) {
            if (Link.connected) Control() else ConnectScreen(prefs, lang) { lang = it; prefs.edit().putString("lang", it).apply() }
        }
    }
}

// ---------- shared widgets ----------
@Composable fun Btn(label: String, mod: Modifier = Modifier, on: Boolean = false, onDown: () -> Unit = {}, onUp: () -> Unit = {}) {
    var p by remember { mutableStateOf(false) }
    val dn by rememberUpdatedState(onDown); val up by rememberUpdatedState(onUp)
    val view = LocalView.current
    val lit = p || on
    Box(mod.heightIn(min = 44.dp).clip(Cut(24f)).background(if (lit) Y else PN).border(1.dp, if (lit) Y else DIM, Cut(24f))
        .pointerInput(Unit) { awaitEachGesture {
            awaitFirstDown(); p = true; view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); dn()
            waitForUpOrCancellation(); p = false; up()
        } }, contentAlignment = Alignment.Center) {
        Text(label, color = if (lit) Color.Black else Y, fontWeight = FontWeight.Bold, fontSize = 15.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 6.dp))
    }
}

@Composable fun Field(label: String, v: String, kb: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) = TextField(v, onChange, Modifier.fillMaxWidth(), singleLine = true,
    label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = kb),
    colors = TextFieldDefaults.colors(focusedContainerColor = PN, unfocusedContainerColor = PN, focusedTextColor = Y, unfocusedTextColor = Y, cursorColor = Y,
        focusedIndicatorColor = Y, unfocusedIndicatorColor = DIM, focusedLabelColor = Y, unfocusedLabelColor = DIM))

// ---------- connect screen ----------
@SuppressLint("MissingPermission")
@Composable fun ConnectScreen(prefs: SharedPreferences, lang: String, setLang: (String) -> Unit) {
    val ctx = LocalContext.current
    var mode by remember { mutableIntStateOf(0) }
    var pin by remember { mutableStateOf(prefs.getString("pin", "") ?: "") }
    var ip by remember { mutableStateOf(prefs.getString("ip", "") ?: "") }
    var err by remember { mutableStateOf<String?>(null) }
    var found by remember { mutableStateOf(listOf<Pair<String, String>>()) }
    var searching by remember { mutableStateOf(false) }
    var searched by remember { mutableStateOf(false) }
    var devs by remember { mutableStateOf(listOf<BluetoothDevice>()) }
    var denied by remember { mutableStateOf(false) }
    var about by remember { mutableStateOf(false) }
    fun loadBt() { devs = try { ctx.getSystemService(BluetoothManager::class.java)?.adapter?.bondedDevices?.toList() ?: listOf() } catch (e: SecurityException) { listOf() } }
    val perm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok -> denied = !ok; if (ok) loadBt() }
    fun needBt() { if (Build.VERSION.SDK_INT >= 31 && ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) perm.launch(Manifest.permission.BLUETOOTH_CONNECT) else loadBt() }
    val done: (String?) -> Unit = { r -> err = r; if (r == null) prefs.edit().putString("pin", pin).putString("ip", ip).apply() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Btn(if (lang == "fa") "English" else "فارسی", Modifier.width(100.dp), onDown = { setLang(if (lang == "fa") "en" else "fa") })
            Btn(tr("about"), Modifier.width(100.dp), onDown = { about = true })
        }
        Spacer(Modifier.height(14.dp))
        Text("WINGART CTRL", color = Y, fontSize = 30.sp, fontWeight = FontWeight.Black, letterSpacing = 3.sp)
        Box(Modifier.width(120.dp).height(3.dp).background(Y))
        Text("v$VERSION", color = DIM)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Btn(tr("wifi"), Modifier.weight(1f), on = mode == 0, onDown = { mode = 0 })
            Btn(tr("bluetooth"), Modifier.weight(1f), on = mode == 1, onDown = { mode = 1; needBt() })
        }
        Field(tr("pin"), pin, KeyboardType.Number) { pin = it.filter(Char::isDigit).take(6) }
        if (mode == 0) {
            Field(tr("ip"), ip, KeyboardType.Uri) { ip = it }
            Btn(if (searching) tr("connecting") else tr("find"), Modifier.fillMaxWidth(), onDown = {
                if (!searching) { searching = true; Link.discover { found = it; searching = false; searched = true; if (it.isNotEmpty()) ip = it[0].second } } })
            found.forEach { (n, a) -> Btn("$n  ·  $a", Modifier.fillMaxWidth(), on = ip == a, onDown = { ip = a }) }
            if (searched && found.isEmpty()) Text(tr("none"), color = DIM)
            Btn(tr("connect"), Modifier.fillMaxWidth().height(54.dp), on = true, onDown = { if (!Link.busy && ip.isNotBlank()) { err = null; Link.connectWifi(ip.trim(), pin, done) } })
        } else {
            if (denied) Text(tr("bt_perm"), color = RED)
            if (devs.isEmpty()) Text(tr("bt_hint"), color = DIM, textAlign = TextAlign.Center)
            devs.forEach { d -> Btn(try { d.name ?: d.address } catch (e: SecurityException) { d.address }, Modifier.fillMaxWidth(), onDown = { if (!Link.busy) { err = null; Link.connectBt(d, pin, done) } }) }
            Btn("↻", Modifier.width(80.dp), onDown = { needBt() })
        }
        if (Link.busy) Text(tr("connecting"), color = Y)
        err?.let { Text(tr("err_$it"), color = RED, fontWeight = FontWeight.Bold) }
    }
    if (about) AlertDialog(onDismissRequest = { about = false }, containerColor = BG, shape = Cut(40f),
        title = { Text("WINGART CTRL", color = Y, fontWeight = FontWeight.Black) },
        text = { Column { Text("${tr("version")} $VERSION", color = DIM); Spacer(Modifier.height(10.dp)); Text(tr("about_text"), color = Y) } },
        confirmButton = { val uri = LocalUriHandler.current; Btn("✈  " + tr("tg"), Modifier.width(190.dp), onDown = { uri.openUri(LINK) }) },
        dismissButton = { Btn(tr("close"), Modifier.width(90.dp), onDown = { about = false }) })
}

// ---------- control screen ----------
@Composable fun Control() {
    var tab by remember { mutableIntStateOf(0) }
    val act = LocalContext.current as Activity
    DisposableEffect(tab) {
        act.requestedOrientation = if (tab == 3) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        onDispose { act.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED }
    }
    Column(Modifier.fillMaxSize().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (tab != 3) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("●  ${Link.hostName}", color = Y, fontWeight = FontWeight.Bold)
                Btn(tr("disconnect"), Modifier.width(140.dp), onDown = { Link.close() })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("trackpad", "keys", "media", "game").forEachIndexed { i, k -> Btn(tr(k), Modifier.weight(1f), on = tab == i, onDown = { tab = i }) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) { when (tab) { 0 -> TrackPad(); 1 -> Keys(); 2 -> Media(); else -> Game { tab = 0 } } }
    }
}

@Composable fun TrackPad() {
    var sens by remember { mutableFloatStateOf(1.8f) }
    val sensS by rememberUpdatedState(sens)
    val dens = LocalDensity.current.density
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.weight(1f).fillMaxWidth().clip(Cut(40f)).background(PN).border(1.dp, Y, Cut(40f)).pointerInput(Unit) {
            awaitEachGesture {
                awaitFirstDown(requireUnconsumed = false)
                val t0 = System.currentTimeMillis(); var moved = 0f; var maxN = 1; var ax = 0f; var ay = 0f; var sy = 0f
                do {
                    val ev = awaitPointerEvent()
                    val down = ev.changes.filter { it.pressed }
                    if (down.size > maxN) maxN = down.size
                    if (down.isNotEmpty()) {
                        var dx = 0f; var dy = 0f
                        for (c in down) { val d = c.positionChange(); dx += d.x; dy += d.y; c.consume() }
                        dx /= down.size * dens; dy /= down.size * dens
                        moved += abs(dx) + abs(dy)
                        if (down.size >= 2) { sy += dy; val s = (sy / 6f).toInt(); if (s != 0) { Link.send("t" to "sc", "y" to s * 40); sy -= s * 6f } }
                        else {
                            val k = sensS * (1f + min(hypot(dx, dy), 40f) / 25f); ax += dx * k; ay += dy * k
                            val ix = ax.toInt(); val iy = ay.toInt()
                            if (ix != 0 || iy != 0) { Link.send("t" to "mv", "x" to ix, "y" to iy); ax -= ix; ay -= iy }
                        }
                    }
                } while (ev.changes.any { it.pressed })
                if (moved < 8f && System.currentTimeMillis() - t0 < 260) Link.send("t" to "cl", "b" to (if (maxN >= 2) "r" else "l"), "s" to "c")
            }
        }, contentAlignment = Alignment.BottomCenter) { Text(tr("pad_hint"), color = DIM, fontSize = 12.sp, modifier = Modifier.padding(10.dp)) }
        Row(Modifier.height(56.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("left" to "l", "mid" to "m", "right" to "r").forEach { (k, b) ->
                Btn(tr(k), Modifier.weight(1f).fillMaxHeight(), onDown = { Link.send("t" to "cl", "b" to b, "s" to "d") }, onUp = { Link.send("t" to "cl", "b" to b, "s" to "u") }) }
        }
        Slider(sens, { sens = it }, valueRange = 1f..4f, colors = SliderDefaults.colors(thumbColor = Y, activeTrackColor = Y, inactiveTrackColor = DIM))
    }
}

@Composable fun Grid(rows: List<List<Pair<String, String>>>, mods: Set<String> = emptySet(), onMod: (String) -> Unit = {}, act: (String) -> Unit) {
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { r -> Row(Modifier.weight(1f).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            r.forEach { (l, c) -> Btn(l, Modifier.weight(1f).fillMaxHeight(), on = c.startsWith("m:") && c.substring(2) in mods,
                onDown = { if (c.startsWith("m:")) onMod(c.substring(2)) else act(c) }) } } }
    }
}

@Composable fun Keys() {
    var mods by remember { mutableStateOf(setOf<String>()) }
    var txt by remember { mutableStateOf("") }
    fun act(code: String) {
        if (code.startsWith("c:")) Link.send("t" to "combo", "keys" to JSONArray(code.substring(2).split("+")))
        else { val k = code.substring(2)
            if (mods.isNotEmpty()) { Link.send("t" to "combo", "keys" to JSONArray(mods.toList() + k)); mods = emptySet() } else Link.send("t" to "k", "k" to k) }
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(txt, { n ->
            if (n.length > txt.length && n.startsWith(txt)) {
                val add = n.substring(txt.length)
                if (mods.isNotEmpty() && add.length == 1) { Link.send("t" to "combo", "keys" to JSONArray(mods.toList() + add)); mods = emptySet() } else Link.send("t" to "tx", "v" to add)
            } else if (n.length < txt.length && txt.startsWith(n)) repeat(txt.length - n.length) { Link.send("t" to "k", "k" to "backspace") }
            txt = if (n.length > 40) "" else n
        }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(tr("type"), color = DIM) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { Link.send("t" to "k", "k" to "enter") }),
            colors = TextFieldDefaults.colors(focusedContainerColor = PN, unfocusedContainerColor = PN, focusedTextColor = Y, unfocusedTextColor = Y, cursorColor = Y, focusedIndicatorColor = Y, unfocusedIndicatorColor = DIM))
        Box(Modifier.weight(1f)) {
            Grid(listOf(
                listOf("⌫" to "k:backspace", "Enter" to "k:enter", "Esc" to "k:esc", "Tab" to "k:tab"),
                listOf("Ctrl" to "m:ctrl", "Alt" to "m:alt", "Shift" to "m:shift", "Win" to "m:win"),
                listOf("Home" to "k:home", "▲" to "k:up", "End" to "k:end", "PgUp" to "k:pgup"),
                listOf("◀" to "k:left", "▼" to "k:down", "▶" to "k:right", "PgDn" to "k:pgdn"),
                listOf("Alt+Tab" to "c:alt+tab", "Copy" to "c:ctrl+c", "Paste" to "c:ctrl+v", "Alt+F4" to "c:alt+f4")),
                mods, { m -> mods = if (m in mods) mods - m else mods + m }, ::act)
        }
    }
}

@Composable fun Media() = Grid(listOf(
    listOf("⏯" to "k:play"), listOf("⏮" to "k:prev", "⏭" to "k:next"), listOf("🔉" to "k:vdown", "🔇" to "k:mute", "🔊" to "k:vup"),
    listOf("−10s" to "k:j", "␣" to "k:space", "+10s" to "k:l"), listOf("−5s" to "k:left", "⛶" to "k:f", "+5s" to "k:right"),
    listOf("CC" to "k:c", "⏭ YT" to "c:shift+n", "<" to "c:shift+,", ">" to "c:shift+."))) { c ->
    if (c.startsWith("c:")) Link.send("t" to "combo", "keys" to JSONArray(c.substring(2).split("+"))) else Link.send("t" to "k", "k" to c.substring(2)) }

// ---------- gamepad ----------
@Composable fun GB(l: String, k: String) = Btn(l, Modifier.width(62.dp), onDown = { Pad.btn(k, true) }, onUp = { Pad.btn(k, false) })

@Composable fun Diamond(mod: Modifier, keys: List<String>, labels: List<String>) {
    val pos = listOf(Alignment.TopCenter, Alignment.CenterStart, Alignment.CenterEnd, Alignment.BottomCenter)
    Box(mod.size(144.dp)) { for (i in 0..3) Btn(labels[i], Modifier.align(pos[i]).size(46.dp), onDown = { Pad.btn(keys[i], true) }, onUp = { Pad.btn(keys[i], false) }) }
}

@Composable fun Stick(mod: Modifier, onMove: (Float, Float) -> Unit) {
    var off by remember { mutableStateOf(Offset.Zero) }
    val cb by rememberUpdatedState(onMove)
    Box(mod.size(140.dp).drawBehind {
        drawCircle(Y.copy(alpha = .10f), radius = size.minDimension / 2); drawCircle(DIM, radius = size.minDimension / 2, style = Stroke(3f))
        drawCircle(Y, radius = size.minDimension * 0.2f, center = center + off)
    }.pointerInput(Unit) {
        awaitEachGesture {
            val first = awaitFirstDown()
            val c = Offset(size.width / 2f, size.height / 2f); val r = size.width * 0.35f
            fun upd(p: Offset) { var v = p - c; val l = v.getDistance(); if (l > r) v = v * (r / l); off = v; cb(v.x / r, -v.y / r) }
            upd(first.position)
            do {
                val ev = awaitPointerEvent(); val ch = ev.changes.firstOrNull { it.id == first.id }
                if (ch != null && ch.pressed) { upd(ch.position); ch.consume() }
            } while (ev.changes.any { it.id == first.id && it.pressed })
            off = Offset.Zero; cb(0f, 0f)
        }
    })
}

@Composable fun Game(exit: () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        if (!Link.gamepadOk) Text(tr("nopad"), color = RED, textAlign = TextAlign.Center, modifier = Modifier.align(Alignment.Center))
        Row(Modifier.align(Alignment.TopStart), horizontalArrangement = Arrangement.spacedBy(6.dp)) { GB("LB", "lb"); GB("LT", "lt") }
        Row(Modifier.align(Alignment.TopEnd), horizontalArrangement = Arrangement.spacedBy(6.dp)) { GB("RT", "rt"); GB("RB", "rb") }
        Row(Modifier.align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(6.dp)) { GB("Back", "back"); Btn("✕", Modifier.width(52.dp), onDown = exit); GB("Start", "start") }
        Stick(Modifier.align(Alignment.BottomStart).padding(start = 10.dp)) { x, y -> Pad.lx = x; Pad.ly = y; Pad.push() }
        Diamond(Modifier.align(Alignment.BottomStart).padding(start = 190.dp, bottom = 4.dp), listOf("up", "left", "right", "down"), listOf("▲", "◀", "▶", "▼"))
        Stick(Modifier.align(Alignment.BottomEnd).padding(end = 190.dp)) { x, y -> Pad.rx = x; Pad.ry = y; Pad.push() }
        Diamond(Modifier.align(Alignment.BottomEnd).padding(end = 10.dp, bottom = 4.dp), listOf("y", "x", "b", "a"), listOf("Y", "X", "B", "A"))
    }
}
