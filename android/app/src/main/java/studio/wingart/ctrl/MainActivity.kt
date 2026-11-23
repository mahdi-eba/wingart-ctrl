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
import android.os.Handler
import android.os.Looper
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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.min

const val VERSION = "1.1.0"
const val LINK = "https://t.me/wingartstudio"
val Y = Color(0xFFFCEE09); val BG = Color(0xFF0A0A0A); val PN = Color(0xFF17170A); val DIM = Color(0xFF8C8506); val RED = Color(0xFFFF5A4D)
val XA = Color(0xFF3FB950); val XB = Color(0xFFE5483B); val XX = Color(0xFF3B82F6)

val STR = mapOf(
    "en" to mapOf("wifi" to "Wi-Fi", "bluetooth" to "Bluetooth", "pin" to "PIN (shown on your PC)", "ip" to "Laptop address", "find" to "Find my laptop",
        "connect" to "Connect", "connecting" to "Connecting…", "disconnect" to "Disconnect", "trackpad" to "Trackpad", "keys" to "Keys", "media" to "Media", "game" to "Game",
        "about" to "About", "version" to "Version", "err_pin" to "Wrong PIN", "err_conn" to "Could not connect", "none" to "No laptop found",
        "bt_hint" to "Pair your laptop in the phone's Bluetooth settings first, then come back.", "bt_perm" to "Bluetooth permission is needed",
        "bt_help" to "Check that the PC app shows Bluetooth: ready, and that this phone is paired with the PC.",
        "type" to "Type here…", "pad_hint" to "Move · tap to click · two fingers to scroll", "nopad" to "Gamepad driver missing on PC (ViGEmBus)",
        "about_text" to "Control your laptop from your phone: trackpad, keyboard, media and gamepad.\nBuilt by Wingart Studio.", "tg" to "Telegram channel", "close" to "Close",
        "left" to "Left", "mid" to "Mid", "right" to "Right", "sens" to "Speed",
        "sys" to "System", "yt" to "YouTube", "prev" to "Previous", "play" to "Play / Pause", "next" to "Next", "vdn" to "Vol −", "vup" to "Vol +", "mute" to "Mute",
        "full" to "Fullscreen", "cc" to "Captions", "slow" to "Slower", "fast" to "Faster", "nextv" to "Next video"),
    "fa" to mapOf("wifi" to "وای‌فای", "bluetooth" to "بلوتوث", "pin" to "کد اتصال (روی PC نمایش داده می‌شود)", "ip" to "آدرس لپ‌تاپ", "find" to "پیدا کردن لپ‌تاپ",
        "connect" to "اتصال", "connecting" to "در حال اتصال…", "disconnect" to "قطع اتصال", "trackpad" to "ترک‌پد", "keys" to "کیبورد", "media" to "مدیا", "game" to "بازی",
        "about" to "درباره ما", "version" to "نسخه", "err_pin" to "کد اشتباه است", "err_conn" to "اتصال برقرار نشد", "none" to "لپ‌تاپی پیدا نشد",
        "bt_hint" to "اول لپ‌تاپ را از تنظیمات بلوتوث گوشی جفت کنید و برگردید.", "bt_perm" to "اجازه‌ی بلوتوث لازم است",
        "bt_help" to "مطمئن شوید برنامه‌ی PC جلوی بلوتوث «آماده» نشان می‌دهد و گوشی با PC جفت (Pair) شده است.",
        "type" to "اینجا بنویسید…", "pad_hint" to "حرکت · لمس برای کلیک · دو انگشت برای اسکرول", "nopad" to "درایور دسته روی PC نصب نیست (ViGEmBus)",
        "about_text" to "لپ‌تاپ را با گوشی کنترل کنید: ترک‌پد، کیبورد، مدیا و دسته‌ی بازی.\nساخته‌شده توسط Wingart Studio.", "tg" to "کانال تلگرام", "close" to "بستن",
        "left" to "چپ", "mid" to "وسط", "right" to "راست", "sens" to "سرعت",
        "sys" to "سیستم", "yt" to "یوتیوب", "prev" to "قبلی", "play" to "پخش / توقف", "next" to "بعدی", "vdn" to "صدا −", "vup" to "صدا +", "mute" to "بی‌صدا",
        "full" to "تمام‌صفحه", "cc" to "زیرنویس", "slow" to "کندتر", "fast" to "سریع‌تر", "nextv" to "ویدیوی بعد"))
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

/** Keyboard / media / gamepad are physical layouts: always left-to-right, even in Persian. */
@Composable fun Ltr(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) { content() }
}

// ---------- shared widgets ----------
@Composable fun Btn(label: String, mod: Modifier = Modifier, on: Boolean = false, icon: Ic? = null, rep: Boolean = false, onDown: () -> Unit = {}, onUp: () -> Unit = {}) {
    var p by remember { mutableStateOf(false) }
    val dn by rememberUpdatedState(onDown); val up by rememberUpdatedState(onUp)
    val view = LocalView.current
    val h = remember { Handler(Looper.getMainLooper()) }
    val job = remember { arrayOfNulls<Runnable>(1) }
    DisposableEffect(Unit) { onDispose { job[0]?.let { h.removeCallbacks(it) } } }
    val lit = p || on
    val fg = if (lit) Color.Black else Y
    Box(mod.heightIn(min = 44.dp).clip(Cut(24f)).background(if (lit) Y else PN).border(1.dp, if (lit) Y else DIM, Cut(24f))
        .pointerInput(Unit) { awaitEachGesture {
            awaitFirstDown(); p = true; view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); dn()
            if (rep) {
                val r = object : Runnable { override fun run() { dn(); h.postDelayed(this, 80) } }
                job[0] = r; h.postDelayed(r, 380)
            }
            waitForUpOrCancellation(); p = false
            job[0]?.let { h.removeCallbacks(it) }; job[0] = null
            up()
        } }, contentAlignment = Alignment.Center) {
        if (icon != null) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Canvas(Modifier.size(if (label.isEmpty()) 26.dp else 24.dp)) { drawIc(icon, fg) }
                if (label.isNotEmpty()) {
                    Spacer(Modifier.height(3.dp))
                    Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, maxLines = 1)
                }
            }
        } else {
            Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = if (label.length > 5) 12.sp else 15.sp, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.padding(horizontal = 4.dp))
        }
    }
}

/** Round Xbox-style face button. */
@Composable fun FaceBtn(label: String, col: Color, sz: Dp, onDown: () -> Unit, onUp: () -> Unit) {
    var p by remember { mutableStateOf(false) }
    val dn by rememberUpdatedState(onDown); val up by rememberUpdatedState(onUp)
    val view = LocalView.current
    Box(Modifier.size(sz).clip(CircleShape).background(if (p) col else PN).border(2.dp, col, CircleShape)
        .pointerInput(Unit) { awaitEachGesture {
            awaitFirstDown(); p = true; view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP); dn()
            waitForUpOrCancellation(); p = false; up()
        } }, contentAlignment = Alignment.Center) {
        Text(label, color = if (p) Color.Black else col, fontWeight = FontWeight.Black, fontSize = 20.sp)
    }
}

@Composable fun Field(label: String, v: String, kb: KeyboardType = KeyboardType.Text, onChange: (String) -> Unit) = TextField(v, onChange, Modifier.fillMaxWidth(), singleLine = true,
    label = { Text(label) }, keyboardOptions = KeyboardOptions(keyboardType = kb),
    colors = TextFieldDefaults.colors(focusedContainerColor = PN, unfocusedContainerColor = PN, focusedTextColor = Y, unfocusedTextColor = Y, cursorColor = Y,
        focusedIndicatorColor = Y, unfocusedIndicatorColor = DIM, focusedLabelColor = Y, unfocusedLabelColor = DIM))

@Composable fun Sec(t: String) {
    Text(t.uppercase(), color = DIM, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, modifier = Modifier.padding(top = 4.dp))
}

/** A row of equal-height buttons. */
@Composable fun KRow(h: Dp = 52.dp, content: @Composable RowScope.() -> Unit) {
    Row(Modifier.fillMaxWidth().height(h), horizontalArrangement = Arrangement.spacedBy(8.dp), content = content)
}

/** An equal-width button inside a KRow. */
@Composable fun RowScope.KB(label: String, icon: Ic? = null, rep: Boolean = false, lit: Boolean = false, onDown: () -> Unit) {
    Btn(label, Modifier.weight(1f).fillMaxHeight(), on = lit, icon = icon, rep = rep, onDown = onDown)
}

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
    fun search() { if (!searching) { searching = true; Link.discover { found = it; searching = false; searched = true; if (it.isNotEmpty()) ip = it[0].second } } }
    val done: (String?) -> Unit = { r -> err = r; if (r == null) prefs.edit().putString("pin", pin).putString("ip", ip).apply() }
    LaunchedEffect(Unit) { if (ip.isBlank()) search() }

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
            Btn(if (searching) tr("connecting") else tr("find"), Modifier.fillMaxWidth(), onDown = { search() })
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
        if (err == "conn" && mode == 1) Text(tr("bt_help"), color = DIM, fontSize = 12.sp, textAlign = TextAlign.Center)
        if (err == "conn" && Link.lastError.isNotEmpty()) Text(Link.lastError, color = DIM, fontSize = 11.sp, textAlign = TextAlign.Center)
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
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(10.dp).clip(CircleShape).background(Y))
                Text(Link.hostName, color = Y, fontWeight = FontWeight.Bold, maxLines = 1, modifier = Modifier.weight(1f))
                Btn("", Modifier.size(44.dp), icon = Ic.REFRESH, onDown = { Pad.release() })
                Btn(tr("disconnect"), Modifier.width(130.dp), onDown = { Link.close() })
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("trackpad", "keys", "media", "game").forEachIndexed { i, k -> Btn(tr(k), Modifier.weight(1f), on = tab == i, onDown = { if (tab != i) { Pad.release(); tab = i } }) }
            }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Ltr { when (tab) { 0 -> TrackPad(); 1 -> Keys(); 2 -> Media(); else -> Game { Pad.release(); tab = 0 } } }
        }
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
                        if (down.size >= 2) { sy += dy; val s = (sy / 6f).toInt(); if (s != 0) { Link.scroll(s * 40); sy -= s * 6f } }
                        else {
                            val k = sensS * (1f + min(hypot(dx, dy), 40f) / 25f); ax += dx * k; ay += dy * k
                            val ix = ax.toInt(); val iy = ay.toInt()
                            if (ix != 0 || iy != 0) { Link.move(ix, iy); ax -= ix; ay -= iy }
                        }
                    }
                } while (ev.changes.any { it.pressed })
                if (moved < 8f && System.currentTimeMillis() - t0 < 260) Link.send("t" to "cl", "b" to (if (maxN >= 2) "r" else "l"), "s" to "c")
            }
        }, contentAlignment = Alignment.BottomCenter) { Text(tr("pad_hint"), color = DIM, fontSize = 12.sp, modifier = Modifier.padding(10.dp)) }
        KRow(56.dp) {
            listOf("left" to "l", "mid" to "m", "right" to "r").forEach { (k, b) ->
                Btn(tr(k), Modifier.weight(1f).fillMaxHeight(), onDown = { Link.send("t" to "cl", "b" to b, "s" to "d") }, onUp = { Link.send("t" to "cl", "b" to b, "s" to "u") }) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(tr("sens"), color = DIM, fontSize = 12.sp)
            Slider(sens, { sens = it }, Modifier.weight(1f), valueRange = 1f..4f, colors = SliderDefaults.colors(thumbColor = Y, activeTrackColor = Y, inactiveTrackColor = DIM))
        }
    }
}

@Composable fun Keys() {
    var mods by remember { mutableStateOf(setOf<String>()) }
    var txt by remember { mutableStateOf("") }
    fun press(k: String) {
        if (mods.isNotEmpty()) { Link.send("t" to "combo", "keys" to JSONArray(mods.toList() + k)); mods = emptySet() }
        else Link.send("t" to "k", "k" to k)
    }
    fun combo(vararg k: String) { Link.send("t" to "combo", "keys" to JSONArray(k.toList())) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TextField(txt, { n ->
            if (n.length > txt.length && n.startsWith(txt)) {
                val add = n.substring(txt.length)
                if (mods.isNotEmpty() && add.length == 1) { Link.send("t" to "combo", "keys" to JSONArray(mods.toList() + add)); mods = emptySet() } else Link.send("t" to "tx", "v" to add)
            } else if (n.length < txt.length && txt.startsWith(n)) repeat(txt.length - n.length) { Link.send("t" to "k", "k" to "backspace") }
            txt = if (n.length > 40) "" else n
        }, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text(tr("type"), color = DIM) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { Link.send("t" to "k", "k" to "enter") }),
            colors = TextFieldDefaults.colors(focusedContainerColor = PN, unfocusedContainerColor = PN, focusedTextColor = Y, unfocusedTextColor = Y, cursorColor = Y, focusedIndicatorColor = Y, unfocusedIndicatorColor = DIM))
        KRow {
            KB("Esc") { press("esc") }
            KB("Tab") { press("tab") }
            KB("", Ic.BKSP, rep = true) { press("backspace") }
            KB("", Ic.ENTER) { press("enter") }
        }
        KRow {
            listOf("Ctrl" to "ctrl", "Alt" to "alt", "Shift" to "shift", "Win" to "win").forEach { (l, m) ->
                KB(l, lit = m in mods) { mods = if (m in mods) mods - m else mods + m }
            }
        }
        Row(Modifier.fillMaxWidth().height(112.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(Modifier.weight(3f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Spacer(Modifier.weight(1f)); KB("", Ic.UP, rep = true) { press("up") }; Spacer(Modifier.weight(1f))
                }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KB("", Ic.LEFT, rep = true) { press("left") }; KB("", Ic.DOWN, rep = true) { press("down") }; KB("", Ic.RIGHT, rep = true) { press("right") }
                }
            }
            Column(Modifier.weight(2f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { KB("Home") { press("home") }; KB("PgUp", rep = true) { press("pgup") } }
                Row(Modifier.fillMaxWidth().weight(1f), horizontalArrangement = Arrangement.spacedBy(8.dp)) { KB("End") { press("end") }; KB("PgDn", rep = true) { press("pgdn") } }
            }
        }
        KRow {
            KB("Copy", Ic.COPY) { combo("ctrl", "c") }
            KB("Paste", Ic.PASTE) { combo("ctrl", "v") }
            KB("Undo") { combo("ctrl", "z") }
            KB("Del", rep = true) { press("del") }
        }
        KRow {
            KB("Alt+Tab") { combo("alt", "tab") }
            KB("Win+D") { combo("win", "d") }
            KB("Alt+F4") { combo("alt", "f4") }
            KB("Ctrl+A") { combo("ctrl", "a") }
        }
    }
}

@Composable fun Media() {
    fun press(k: String) { Link.send("t" to "k", "k" to k) }
    fun combo(vararg k: String) { Link.send("t" to "combo", "keys" to JSONArray(k.toList())) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Sec(tr("sys"))
        KRow(78.dp) {
            KB(tr("prev"), Ic.PREV) { press("prev") }
            KB(tr("play"), Ic.PLAY) { press("play") }
            KB(tr("next"), Ic.NEXT) { press("next") }
        }
        KRow(78.dp) {
            KB(tr("vdn"), Ic.VDOWN, rep = true) { press("vdown") }
            KB(tr("mute"), Ic.MUTE) { press("mute") }
            KB(tr("vup"), Ic.VUP, rep = true) { press("vup") }
        }
        Sec(tr("yt"))
        KRow(78.dp) {
            KB("10s", Ic.REW) { press("j") }
            KB(tr("full"), Ic.FULL) { press("f") }
            KB("10s", Ic.FWD) { press("l") }
        }
        KRow(78.dp) {
            KB("5s", Ic.REW) { press("left") }
            KB(tr("cc"), Ic.CC) { press("c") }
            KB("5s", Ic.FWD) { press("right") }
        }
        KRow(78.dp) {
            KB(tr("slow"), Ic.SLOW) { combo("shift", ",") }
            KB(tr("nextv"), Ic.NEXT) { combo("shift", "n") }
            KB(tr("fast"), Ic.FAST) { combo("shift", ".") }
        }
    }
}

// ---------- gamepad (Xbox layout) ----------
@Composable fun GB(l: String, k: String, w: Dp = 66.dp) = Btn(l, Modifier.width(w).height(40.dp), onDown = { Pad.btn(k, true) }, onUp = { Pad.btn(k, false) })

@Composable fun DPad(mod: Modifier, sz: Dp) {
    val b = sz * 0.38f
    Box(mod.size(sz)) {
        Btn("", Modifier.align(Alignment.TopCenter).size(b), icon = Ic.UP, onDown = { Pad.btn("up", true) }, onUp = { Pad.btn("up", false) })
        Btn("", Modifier.align(Alignment.CenterStart).size(b), icon = Ic.LEFT, onDown = { Pad.btn("left", true) }, onUp = { Pad.btn("left", false) })
        Btn("", Modifier.align(Alignment.CenterEnd).size(b), icon = Ic.RIGHT, onDown = { Pad.btn("right", true) }, onUp = { Pad.btn("right", false) })
        Btn("", Modifier.align(Alignment.BottomCenter).size(b), icon = Ic.DOWN, onDown = { Pad.btn("down", true) }, onUp = { Pad.btn("down", false) })
    }
}

@Composable fun Faces(mod: Modifier, sz: Dp) {
    val b = sz * 0.38f
    Box(mod.size(sz)) {
        Box(Modifier.align(Alignment.TopCenter)) { FaceBtn("Y", Y, b, { Pad.btn("y", true) }, { Pad.btn("y", false) }) }
        Box(Modifier.align(Alignment.CenterStart)) { FaceBtn("X", XX, b, { Pad.btn("x", true) }, { Pad.btn("x", false) }) }
        Box(Modifier.align(Alignment.CenterEnd)) { FaceBtn("B", XB, b, { Pad.btn("b", true) }, { Pad.btn("b", false) }) }
        Box(Modifier.align(Alignment.BottomCenter)) { FaceBtn("A", XA, b, { Pad.btn("a", true) }, { Pad.btn("a", false) }) }
    }
}

@Composable fun Stick(mod: Modifier, sz: Dp, onMove: (Float, Float) -> Unit) {
    var off by remember { mutableStateOf(Offset.Zero) }
    val cb by rememberUpdatedState(onMove)
    Box(mod.size(sz).drawBehind {
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
    DisposableEffect(Unit) { Pad.release(); onDispose { Pad.release() } }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val w = maxWidth; val h = maxHeight
        val sk = minOf(h * 0.38f, 136.dp); val pd = minOf(h * 0.36f, 124.dp); val fc = minOf(h * 0.40f, 140.dp)
        if (!Link.gamepadOk) Text(tr("nopad"), color = RED, textAlign = TextAlign.Center, fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp))
        // shoulders + triggers
        Row(Modifier.align(Alignment.TopStart).padding(start = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { GB("LT", "lt"); GB("LB", "lb") }
        Row(Modifier.align(Alignment.TopEnd).padding(end = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { GB("RB", "rb"); GB("RT", "rt") }
        // centre: Back · refresh · close · Start
        Row(Modifier.align(Alignment.TopCenter), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GB("Back", "back", 60.dp)
            Btn("", Modifier.size(width = 44.dp, height = 40.dp), icon = Ic.REFRESH, onDown = { Pad.release() })
            Btn("", Modifier.size(width = 44.dp, height = 40.dp), icon = Ic.CLOSE, onDown = exit)
            GB("Start", "start", 60.dp)
        }
        // left side: left stick (top) + D-pad (bottom)
        Stick(Modifier.align(Alignment.TopStart).offset(x = w * 0.05f, y = h * 0.17f), sk) { x, y -> Pad.lx = x; Pad.ly = y; Pad.push() }
        DPad(Modifier.align(Alignment.TopStart).offset(x = w * 0.21f, y = h * 0.58f), pd)
        // right side: face buttons (top) + right stick (bottom)
        Faces(Modifier.align(Alignment.TopEnd).offset(x = -(w * 0.05f), y = h * 0.17f), fc)
        Stick(Modifier.align(Alignment.TopEnd).offset(x = -(w * 0.21f), y = h * 0.58f), sk) { x, y -> Pad.rx = x; Pad.ry = y; Pad.push() }
    }
}
