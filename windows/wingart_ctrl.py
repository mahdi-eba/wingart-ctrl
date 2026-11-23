"""Wingart Ctrl - Windows host. Phone -> trackpad / keyboard / media / Xbox gamepad over Wi-Fi or Bluetooth."""
import asyncio, ctypes, json, os, random, secrets, socket, sys, threading, time, traceback, webbrowser, winreg
import tkinter as tk
import pystray
from PIL import Image, ImageDraw

VERSION, APP, LINK = "1.1.0", "Wingart Ctrl", "https://t.me/wingartstudio"
TCP_PORT, DISC_PORT = 8765, 8766
BT_UUID = "7c2f6e3a-5b0d-4c1e-9a58-3f9d2b6a1e47"
CFGP = os.path.join(os.environ.get("APPDATA", "."), "WingartCtrl.json")
try: CFG = json.load(open(CFGP))
except Exception: CFG = {}
CFG.setdefault("pin", f"{secrets.randbelow(10**6):06d}"); CFG.setdefault("lang", "en")
def cfg_save():
    try: json.dump(CFG, open(CFGP, "w"))
    except Exception: pass
cfg_save()
ST = {"wifi": "…", "bt": "…", "bt_err": "", "clients": 0}
LOGP = os.path.join(os.environ.get("APPDATA", "."), "WingartCtrl.log")
def log(msg):
    try:
        if os.path.exists(LOGP) and os.path.getsize(LOGP) > 200000: os.remove(LOGP)
        open(LOGP, "a", encoding="utf-8").write(time.strftime("%H:%M:%S ") + str(msg) + "\n")
    except Exception: pass

# ---------------- Windows input ----------------
user32 = ctypes.windll.user32
ULP = ctypes.POINTER(ctypes.c_ulong)
class MI(ctypes.Structure): _fields_ = [("dx", ctypes.c_long), ("dy", ctypes.c_long), ("data", ctypes.c_ulong), ("flags", ctypes.c_ulong), ("time", ctypes.c_ulong), ("extra", ULP)]
class KI(ctypes.Structure): _fields_ = [("vk", ctypes.c_ushort), ("scan", ctypes.c_ushort), ("flags", ctypes.c_ulong), ("time", ctypes.c_ulong), ("extra", ULP)]
class _U(ctypes.Union): _fields_ = [("mi", MI), ("ki", KI)]
class INPUT(ctypes.Structure): _fields_ = [("type", ctypes.c_ulong), ("u", _U)]
def _send(i): user32.SendInput(1, ctypes.byref(i), ctypes.sizeof(INPUT))
def mouse(flags, dx=0, dy=0, data=0): _send(INPUT(0, _U(mi=MI(dx, dy, data & 0xFFFFFFFF, flags, 0, None))))
EXT_VK = {0x21, 0x22, 0x23, 0x24, 0x25, 0x26, 0x27, 0x28, 0x2D, 0x2E, 0x5B, 0x5C, 0x6F, 0x90}
def key_vk(vk, up=False):
    sc = user32.MapVirtualKeyW(vk, 0) & 0xFF
    _send(INPUT(1, _U(ki=KI(vk, sc, (0x1 if vk in EXT_VK else 0) | (0x2 if up else 0), 0, None))))
def key_char(ch):
    for up in (False, True): _send(INPUT(1, _U(ki=KI(0, ord(ch), 0x4 | (0x2 if up else 0), 0, None))))
VK = {"backspace": 8, "tab": 9, "enter": 13, "shift": 16, "ctrl": 17, "alt": 18, "esc": 27, "space": 32, "pgup": 33, "pgdn": 34,
      "end": 35, "home": 36, "left": 37, "up": 38, "right": 39, "down": 40, "del": 46, "win": 0x5B, "mute": 0xAD, "vdown": 0xAE,
      "vup": 0xAF, "next": 0xB0, "prev": 0xB1, "play": 0xB3}
VK.update({f"f{i}": 0x6F + i for i in range(1, 13)})
OEM = {",": 0xBC, ".": 0xBE, "-": 0xBD, "=": 0xBB, ";": 0xBA, "/": 0xBF, "[": 0xDB, "]": 0xDD, "\\": 0xDC, "'": 0xDE, "`": 0xC0}
def vk_of(n):
    """Layout independent: letters/digits map straight to their VK code (works even with a Persian keyboard active)."""
    if n in VK: return VK[n]
    if len(n) == 1:
        c = n.upper()
        if "A" <= c <= "Z" or "0" <= c <= "9": return ord(c)
        if n in OEM: return OEM[n]
    return user32.VkKeyScanW(ord(n[0])) & 0xFF
BTN = {"l": (0x2, 0x4), "r": (0x8, 0x10), "m": (0x20, 0x40)}
HELD_KEYS, HELD_MB = set(), set()
def tap_key(v):
    key_vk(v); time.sleep(0.012); key_vk(v, True)

pad = None; GB = {}
try:
    import vgamepad as vg
    pad = vg.VX360Gamepad(); X = vg.XUSB_BUTTON
    GB = {"a": X.XUSB_GAMEPAD_A, "b": X.XUSB_GAMEPAD_B, "x": X.XUSB_GAMEPAD_X, "y": X.XUSB_GAMEPAD_Y,
          "lb": X.XUSB_GAMEPAD_LEFT_SHOULDER, "rb": X.XUSB_GAMEPAD_RIGHT_SHOULDER, "start": X.XUSB_GAMEPAD_START, "back": X.XUSB_GAMEPAD_BACK,
          "up": X.XUSB_GAMEPAD_DPAD_UP, "down": X.XUSB_GAMEPAD_DPAD_DOWN, "left": X.XUSB_GAMEPAD_DPAD_LEFT, "right": X.XUSB_GAMEPAD_DPAD_RIGHT}
except Exception: pad = None

def release_all():
    for v in list(HELD_KEYS):
        try: key_vk(v, True)
        except Exception: pass
    HELD_KEYS.clear()
    for bt in list(HELD_MB):
        try: mouse(BTN[bt][1])
        except Exception: pass
    HELD_MB.clear()
    if pad:
        try: pad.reset(); pad.update()
        except Exception: pass

def clamp(v, lo, hi): return max(lo, min(hi, v))
def handle(m):
    t = m.get("t")
    if t == "mv": mouse(0x1, clamp(int(m["x"]), -600, 600), clamp(int(m["y"]), -600, 600))
    elif t == "sc":
        if m.get("y"): mouse(0x800, data=clamp(int(m["y"]), -2400, 2400))
        if m.get("x"): mouse(0x1000, data=clamp(int(m["x"]), -2400, 2400))
    elif t == "cl":
        bt = m.get("b", "l")
        if bt not in BTN: return
        d, u = BTN[bt]; s = m.get("s", "c")
        if s == "c": mouse(d); time.sleep(0.01); mouse(u)
        elif s == "d": mouse(d); HELD_MB.add(bt)
        elif s == "u": mouse(u); HELD_MB.discard(bt)
    elif t == "k":
        v = vk_of(m["k"]); s = m.get("s", "c")
        if s == "c": tap_key(v)
        elif s == "d": key_vk(v); HELD_KEYS.add(v)
        elif s == "u": key_vk(v, True); HELD_KEYS.discard(v)
    elif t == "combo":
        vs = [vk_of(k) for k in m["keys"]]
        for v in vs: key_vk(v)
        time.sleep(0.015)
        for v in reversed(vs): key_vk(v, True)
    elif t == "tx":
        for ch in m["v"]: key_char(ch)
    elif t == "reset": release_all()
    elif t == "gp" and pad and m["b"] in GB:
        (pad.press_button if m["s"] == "d" else pad.release_button)(button=GB[m["b"]]); pad.update()
    elif t == "gs" and pad:
        pad.left_joystick_float(clamp(float(m["lx"]), -1.0, 1.0), clamp(float(m["ly"]), -1.0, 1.0))
        pad.right_joystick_float(clamp(float(m["rx"]), -1.0, 1.0), clamp(float(m["ry"]), -1.0, 1.0))
        pad.left_trigger_float(clamp(float(m["lt"]), 0.0, 1.0)); pad.right_trigger_float(clamp(float(m["rt"]), 0.0, 1.0)); pad.update()
    # "p" (heartbeat) and unknown types are ignored

# ---------------- Transport-agnostic session (Wi-Fi TCP and Bluetooth RFCOMM share it) ----------------
async def serve(read, write):
    buf, ok, hb = b"", False, False
    try:
        while True:
            data = await (asyncio.wait_for(read(), 15) if hb else read())
            if not data: break
            buf += data
            while b"\n" in buf:
                line, buf = buf.split(b"\n", 1)
                try: m = json.loads(line)
                except Exception: continue
                if not ok:
                    if m.get("t") == "auth" and str(m.get("pin")) == CFG["pin"]:
                        ok = True; hb = bool(m.get("hb")); ST["clients"] += 1
                        await write(json.dumps({"hello": True, "gamepad": pad is not None, "name": socket.gethostname()}).encode() + b"\n")
                    else:
                        await write(b'{"err":"pin"}\n'); return
                else:
                    try: handle(m)
                    except Exception as e: log(f"handle {m!r}: {e!r}")
    except Exception as e:
        if not isinstance(e, asyncio.TimeoutError): log(f"session: {e!r}")
    finally:
        if ok:
            ST["clients"] -= 1
            release_all()

async def tcp_cb(r, w):
    try:
        sk = w.get_extra_info("socket"); sk.setsockopt(socket.IPPROTO_TCP, socket.TCP_NODELAY, 1); sk.setsockopt(socket.SOL_SOCKET, socket.SO_KEEPALIVE, 1)
    except Exception: pass
    async def rd(): return await r.read(4096)
    async def wr(d): w.write(d); await w.drain()
    await serve(rd, wr)
    w.close()

class Disc(asyncio.DatagramProtocol):
    def connection_made(self, tr): self.tr = tr
    def datagram_received(self, data, addr):
        if data.strip() == b"WINGART?": self.tr.sendto(f"WINGART|{socket.gethostname()}|{TCP_PORT}".encode(), addr)

async def bt_start(loop):
    import uuid
    from winrt.windows.devices.bluetooth.rfcomm import RfcommServiceProvider, RfcommServiceId
    from winrt.windows.networking.sockets import StreamSocketListener, SocketProtectionLevel
    from winrt.windows.storage.streams import DataReader, DataWriter, InputStreamOptions
    async def bt_session(sock):
        rd = DataReader(sock.input_stream); rd.input_stream_options = InputStreamOptions.PARTIAL
        wr = DataWriter(sock.output_stream)
        async def read():
            try: n = await rd.load_async(1024)
            except Exception: return b""
            if not n: return b""
            b = bytearray(n); rd.read_bytes(b); return bytes(b)
        async def write(d): wr.write_bytes(d); await wr.store_async()
        await serve(read, write)
        try: sock.close()
        except Exception: pass
    provider = await RfcommServiceProvider.create_async(RfcommServiceId.from_uuid(uuid.UUID(BT_UUID)))
    listener = StreamSocketListener()
    listener.add_connection_received(lambda s, a: asyncio.run_coroutine_threadsafe(bt_session(a.socket), loop))
    await listener.bind_service_name_async(provider.service_id.as_string(), SocketProtectionLevel.BLUETOOTH_ENCRYPTION_ALLOW_NULL_ENCRYPTION)
    provider.start_advertising(listener)
    globals()["_bt_keep"] = (provider, listener)

async def bt_loop(loop):
    """Keeps trying (e.g. when Bluetooth is switched on later) and remembers the real error for the UI + log."""
    while True:
        try:
            await bt_start(loop); ST["bt"] = "ok"; ST["bt_err"] = ""; log("bluetooth: advertising"); return
        except Exception as e:
            ST["bt"] = "err"; ST["bt_err"] = f"{type(e).__name__}: {e}"[:90]; log("bluetooth: " + traceback.format_exc())
        await asyncio.sleep(20)

async def start_all(loop):
    try:
        await asyncio.start_server(tcp_cb, "0.0.0.0", TCP_PORT); await loop.create_datagram_endpoint(Disc, local_addr=("0.0.0.0", DISC_PORT), allow_broadcast=True)
        ST["wifi"] = "ok"
    except Exception as e: ST["wifi"] = "err"; log(f"wifi: {e!r}")
    asyncio.ensure_future(bt_loop(loop))

def net_thread():
    loop = asyncio.new_event_loop(); asyncio.set_event_loop(loop)
    loop.run_until_complete(start_all(loop)); loop.run_forever()

def lan_ip():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try: s.connect(("10.255.255.255", 1)); return s.getsockname()[0]
    except Exception: return "127.0.0.1"
    finally: s.close()

# ---------------- GUI (canvas-drawn cyberpunk UI) ----------------
S = {"en": {"ver": "Version", "wifi": "Wi-Fi address", "bt": "Bluetooth", "pad": "Virtual gamepad", "dev": "Connected phones", "pin": "PAIRING PIN",
            "hint": "Enter this PIN in the phone app", "newpin": "NEW PIN", "lang": "فارسی", "about": "ABOUT", "tray": "HIDE", "auto": "Start with Windows",
            "ok": "ready", "err": "unavailable", "nodrv": "driver missing", "abt": "Control your laptop from your phone:\ntrackpad, keyboard, media and gamepad.\n\nBuilt by Wingart Studio.",
            "tg": "Telegram channel", "close": "CLOSE"},
     "fa": {"ver": "نسخه", "wifi": "آدرس وای‌فای", "bt": "بلوتوث", "pad": "دسته‌ی مجازی", "dev": "گوشی‌های متصل", "pin": "کد اتصال",
            "hint": "این کد را در اپ گوشی وارد کنید", "newpin": "کد جدید", "lang": "English", "about": "درباره ما", "tray": "مخفی", "auto": "اجرا با ویندوز",
            "ok": "آماده", "err": "غیرفعال", "nodrv": "درایور نصب نیست", "abt": "لپ‌تاپ را با گوشی کنترل کنید:\nترک‌پد، کیبورد، مدیا و دسته‌ی بازی.\n\nساخته‌شده توسط Wingart Studio.",
            "tg": "کانال تلگرام", "close": "بستن"}}
Y, BG, PN, DIM, RED, GRID = "#fcee09", "#070707", "#12120a", "#8c8506", "#ff4d3d", "#13130a"
W, H = 460, 712
RUNKEY = r"Software\Microsoft\Windows\CurrentVersion\Run"

def res(n): return os.path.join(getattr(sys, "_MEIPASS", os.path.dirname(os.path.abspath(__file__))), n)
def font(sz, bold=True, fa=False, mono=False):
    return ("Consolas" if mono else ("Segoe UI" if fa else "Bahnschrift"), sz, "bold" if bold else "normal")

def make_icon(s=64):
    im = Image.new("RGBA", (s, s), (0, 0, 0, 0)); d = ImageDraw.Draw(im); P = lambda *p: [(p[i] * s, p[i + 1] * s) for i in range(0, len(p), 2)]
    d.polygon(P(.04, .04, .78, .04, .96, .22, .96, .96, .22, .96, .04, .78), fill=(252, 238, 9, 255))
    d.line(P(.2, .28, .34, .76, .5, .42, .66, .76, .8, .28), fill=(7, 7, 7, 255), width=max(2, int(s * .1)), joint="curve"); return im

def theme_window(w):
    """Dark/yellow title bar (Windows 11 honours the colors, Windows 10 gets dark mode)."""
    try:
        w.update_idletasks(); hwnd = ctypes.windll.user32.GetParent(w.winfo_id()); dw = ctypes.windll.dwmapi.DwmSetWindowAttribute
        for a, v in ((20, 1), (35, 0x00070707), (36, 0x0009EEFC), (34, 0x0009EEFC)):
            val = ctypes.c_int(v); dw(hwnd, a, ctypes.byref(val), 4)
    except Exception: pass

def cut_poly(c, x, y, w, h, k=14, **kw):
    return c.create_polygon([x, y, x + w - k, y, x + w, y + k, x + w, y + h, x + k, y + h, x, y + h - k], **kw)

def logo(c, x, y, s, tags=()):
    P = lambda *p: [v for i in range(0, len(p), 2) for v in (x + p[i] * s, y + p[i + 1] * s)]
    c.create_polygon(P(0, 0, .78, 0, 1, .22, 1, 1, .22, 1, 0, .78), fill=Y, outline="", tags=tags)
    c.create_line(P(.2, .28, .34, .76, .5, .42, .66, .76, .8, .28), fill=BG, width=max(3, s * .09), joinstyle="round", capstyle="round", tags=tags)

def grid(c, w, h):
    for x in range(0, w, 24): c.create_line(x, 0, x, h, fill=GRID)
    for y in range(0, h, 24): c.create_line(0, y, w, y, fill=GRID)
    for x, y, dx, dy in ((10, 10, 1, 1), (w - 10, 10, -1, 1), (10, h - 10, 1, -1), (w - 10, h - 10, -1, -1)):
        c.create_line(x, y + dy * 22, x, y, x + dx * 22, y, fill=Y, width=2)

def neon_button(c, key, x, y, w, h, text, cmd, fa=False, solid=False, size=11):
    tp, tt = key + "_p", key + "_t"; f0, t0 = (Y, BG) if solid else (PN, Y)
    cut_poly(c, x, y, w, h, 10, fill=f0, outline=Y, width=2, tags=("dyn", tp, key))
    c.create_text(x + w / 2, y + h / 2, text=text, fill=t0, font=font(size, True, fa), tags=("dyn", tt, key))
    def on(e): c.itemconfig(tp, fill=Y); c.itemconfig(tt, fill=BG); c.config(cursor="hand2")
    def off(e): c.itemconfig(tp, fill=f0); c.itemconfig(tt, fill=t0); c.config(cursor="")
    c.tag_bind(key, "<Enter>", on); c.tag_bind(key, "<Leave>", off); c.tag_bind(key, "<Button-1>", lambda e: cmd())

class UI:
    def __init__(self):
        r = self.r = tk.Tk(); r.title(APP); r.geometry(f"{W}x{H}"); r.configure(bg=BG); r.resizable(False, False)
        try: r.iconbitmap(res("wingart.ico"))
        except Exception: pass
        r.protocol("WM_DELETE_WINDOW", r.withdraw)
        c = self.c = tk.Canvas(r, width=W, height=H, bg=BG, highlightthickness=0); c.pack()
        theme_window(r)
        grid(c, W, H); self.scan = c.create_rectangle(0, 0, W, 3, fill="#201d02", outline=""); self.sy = 0; self.glitch = 0
        logo(c, 34, 30, 58)
        self.sh = c.create_text(113, 51, text="WINGART", anchor="w", fill=DIM, font=font(30))
        c.create_text(110, 48, text="WINGART", anchor="w", fill=Y, font=font(30))
        c.create_text(112, 84, text="C   T   R   L", anchor="w", fill=Y, font=font(13))
        c.create_line(30, 118, W - 30, 118, fill=Y, width=2)
        self.av = self.auto_on(); self.sig = None
        self.tick(); self.anim()
        menu = pystray.Menu(pystray.MenuItem("Wingart Ctrl", lambda i, it: self.r.after(0, self.show), default=True), pystray.MenuItem("Quit", self.quit))
        self.icon = pystray.Icon("WingartCtrl", make_icon(64), APP, menu); self.icon.run_detached()
        if "--tray" in sys.argv: r.withdraw()

    def L(self, k): return S[CFG["lang"]][k]
    def draw(self, ip):
        c = self.c; c.delete("dyn"); fa = CFG["lang"] == "fa"; T = "dyn"
        cut_poly(c, W - 124, 36, 90, 28, 8, fill=PN, outline=DIM, width=1, tags=T)
        c.create_text(W - 79, 50, text=f"v{VERSION}", fill=Y, font=font(11, mono=True), tags=T)
        cut_poly(c, 30, 134, W - 60, 214, 18, fill=PN, outline=Y, width=2, tags=T)
        c.create_text(W - 52 if fa else 52, 154, text=self.L("pin"), anchor="e" if fa else "w", fill=DIM, font=font(10, True, fa), tags=T)
        x0 = (W - 352) // 2
        for i, ch in enumerate(CFG["pin"]):
            bx = x0 + i * 60
            cut_poly(c, bx, 176, 52, 66, 8, fill=BG, outline=Y, width=2, tags=T)
            c.create_text(bx + 26, 209, text=ch, fill=Y, font=font(32, mono=True), tags=T)
        c.create_text(W / 2, 262, text=self.L("hint"), fill=DIM, font=font(10, False, fa), tags=T)
        neon_button(c, "b_new", W / 2 - 70, 284, 140, 34, self.L("newpin"), self.newpin, fa, size=10)
        cut_poly(c, 30, 366, W - 60, 196, 18, fill=PN, outline=Y, width=2, tags=T)
        rows = [("wifi", Y if ST["wifi"] == "ok" else RED, f"{ip}:{TCP_PORT}" if ST["wifi"] == "ok" else self.L("err")),
                ("bt", Y if ST["bt"] == "ok" else RED, self.L("ok") if ST["bt"] == "ok" else (ST["bt_err"].split(":")[-1].strip()[:26] or self.L("err"))),
                ("pad", Y if pad else RED, self.L("ok") if pad else self.L("nodrv")),
                ("dev", Y if ST["clients"] else DIM, str(ST["clients"]))]
        for i, (k, col, val) in enumerate(rows):
            y = 398 + i * 44
            if i: c.create_line(50, y - 22, W - 50, y - 22, fill="#2a2705", tags=T)
            dx = W - 54 if fa else 54
            c.create_oval(dx - 5, y - 5, dx + 5, y + 5, fill=col, outline="", tags=T)
            c.create_text(dx - 18 if fa else dx + 18, y, text=self.L(k), anchor="e" if fa else "w", fill=DIM, font=font(11, False, fa), tags=T)
            c.create_text(54 if fa else W - 54, y, text=val, anchor="w" if fa else "e", fill=col if col != DIM else Y, font=font(11, True, fa), tags=T)
        y = 592; sx = 54 if fa else W - 54 - 48
        c.create_text(W - 54 if fa else 54, y, text=self.L("auto"), anchor="e" if fa else "w", fill=Y, font=font(11, False, fa), tags=(T, "b_auto"))
        cut_poly(c, sx, y - 13, 48, 26, 7, fill=Y if self.av else PN, outline=Y, width=2, tags=(T, "b_auto"))
        kx = sx + 27 if self.av else sx + 5
        c.create_rectangle(kx, y - 8, kx + 16, y + 8, fill=BG if self.av else Y, outline="", tags=(T, "b_auto"))
        c.tag_bind("b_auto", "<Button-1>", lambda e: self.toggle_auto()); c.tag_bind("b_auto", "<Enter>", lambda e: c.config(cursor="hand2")); c.tag_bind("b_auto", "<Leave>", lambda e: c.config(cursor=""))
        neon_button(c, "b_lang", 30, 624, 120, 40, self.L("lang"), self.toggle_lang, fa)
        neon_button(c, "b_about", 170, 624, 120, 40, self.L("about"), self.about, fa, solid=True)
        neon_button(c, "b_hide", 310, 624, 120, 40, self.L("tray"), self.r.withdraw, fa)
        c.create_text(W / 2, 686, text="WINGART STUDIO   ·   t.me/wingartstudio", fill=DIM, font=font(9), tags=(T, "b_tg"))
        c.tag_bind("b_tg", "<Button-1>", lambda e: webbrowser.open(LINK))
        c.tag_bind("b_tg", "<Enter>", lambda e: (c.itemconfig("b_tg", fill=Y), c.config(cursor="hand2"))); c.tag_bind("b_tg", "<Leave>", lambda e: (c.itemconfig("b_tg", fill=DIM), c.config(cursor="")))

    def tick(self):
        ip = lan_ip(); sig = (CFG["lang"], CFG["pin"], ST["wifi"], ST["bt"], ST["bt_err"], ST["clients"], pad is not None, self.av, ip)
        if sig != self.sig: self.sig = sig; self.draw(ip)
        self.r.after(1000, self.tick)
    def anim(self):
        if self.r.state() in ("withdrawn", "iconic"): self.r.after(800, self.anim); return
        c = self.c; self.sy = (self.sy + 5) % H; c.coords(self.scan, 0, self.sy, W, self.sy + 3)
        if random.random() < 0.03: self.glitch = 2
        if self.glitch: self.glitch -= 1; c.coords(self.sh, 110 + random.choice((-5, -3, 4, 6)), 48 + random.choice((-2, 2)))
        else: c.coords(self.sh, 113, 51)
        self.r.after(90, self.anim)

    def newpin(self): CFG["pin"] = f"{secrets.randbelow(10**6):06d}"; cfg_save(); self.tick_now()
    def toggle_lang(self): CFG["lang"] = "fa" if CFG["lang"] == "en" else "en"; cfg_save(); self.tick_now()
    def tick_now(self): self.sig = None; self.draw(lan_ip()); self.sig = (CFG["lang"], CFG["pin"], ST["wifi"], ST["bt"], ST["bt_err"], ST["clients"], pad is not None, self.av, lan_ip())
    def auto_on(self):
        try:
            with winreg.OpenKey(winreg.HKEY_CURRENT_USER, RUNKEY) as k: winreg.QueryValueEx(k, "WingartCtrl"); return True
        except OSError: return False
    def toggle_auto(self):
        with winreg.OpenKey(winreg.HKEY_CURRENT_USER, RUNKEY, 0, winreg.KEY_SET_VALUE) as k:
            if self.auto_on():
                try: winreg.DeleteValue(k, "WingartCtrl")
                except OSError: pass
            else:
                cmd = f'"{sys.executable}" --tray' if getattr(sys, "frozen", False) else f'"{sys.executable.replace("python.exe", "pythonw.exe")}" "{os.path.abspath(__file__)}" --tray'
                winreg.SetValueEx(k, "WingartCtrl", 0, winreg.REG_SZ, cmd)
        self.av = self.auto_on(); self.tick_now()
    def show(self): self.r.deiconify(); self.r.lift()

    def about(self):
        if getattr(self, "ab", None) and self.ab.winfo_exists(): self.ab.lift(); return
        w = self.ab = tk.Toplevel(self.r); w.title(self.L("about")); w.configure(bg=BG); w.geometry("400x430"); w.resizable(False, False)
        try: w.iconbitmap(res("wingart.ico"))
        except Exception: pass
        c = tk.Canvas(w, width=400, height=430, bg=BG, highlightthickness=0); c.pack(); theme_window(w); grid(c, 400, 430); fa = CFG["lang"] == "fa"
        logo(c, 150, 36, 100)
        c.create_text(200, 170, text="WINGART CTRL", fill=Y, font=font(24))
        c.create_text(200, 202, text=f"{self.L('ver')} {VERSION}", fill=DIM, font=font(11, False, fa))
        c.create_text(200, 268, text=self.L("abt"), fill=Y, width=340, justify="center", font=font(11, False, fa))
        neon_button(c, "b_tg", 60, 330, 280, 44, "✈   " + self.L("tg"), lambda: webbrowser.open(LINK), fa, solid=True)
        neon_button(c, "b_x", 140, 384, 120, 32, self.L("close"), w.destroy, fa, size=10)
    def quit(self, *a): self.icon.stop(); self.r.after(0, self.r.destroy); os._exit(0)
    def run(self): self.r.mainloop()

def selftest(out):
    lines = []
    for mod in ("winrt.windows.devices.bluetooth.rfcomm", "winrt.windows.networking.sockets", "winrt.windows.storage.streams", "vgamepad", "pystray", "PIL"):
        try: __import__(mod); lines.append("OK " + mod)
        except Exception as e: lines.append(f"FAIL {mod}: {e!r}")
    open(out, "w", encoding="utf-8").write("\n".join(lines))

if __name__ == "__main__":
    if "--selftest" in sys.argv: selftest(sys.argv[sys.argv.index("--selftest") + 1]); sys.exit(0)
    try: ctypes.windll.shell32.SetCurrentProcessExplicitAppUserModelID("WingartStudio.WingartCtrl")
    except Exception: pass
    try:
        threading.Thread(target=net_thread, daemon=True).start()
        UI().run()
    except Exception:
        import traceback; msg = traceback.format_exc()
        try: open(os.path.join(os.environ.get("APPDATA", "."), "WingartCtrl.log"), "w").write(msg)
        except Exception: pass
        ctypes.windll.user32.MessageBoxW(0, msg[-900:], "Wingart Ctrl - error", 0x10)
