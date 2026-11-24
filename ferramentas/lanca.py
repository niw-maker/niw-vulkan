"""Abre o Minecraft 1.20.1 Forge da bancada (pasta isolada), sem launcher, com conta local de teste.

uso: lanca.py --inst PASTA [--xmx 4G] [--prop chave=valor]... [--env CHAVE=VALOR]... [--jvm FLAG]... [--limite 900]
Baixa bibliotecas e recursos dos servidores oficiais da Mojang na primeira vez (com SHA-1 conferido).
Nunca encerra processos que não abriu. Devolve o código de saída do jogo (ou 124 se estourar o limite).
"""
import argparse, concurrent.futures as cf, ctypes, hashlib, json, os, subprocess, sys, time, urllib.request
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
MC = RAIZ / "bancada" / "mc"
FORGE = "1.20.1-forge-47.4.20"
JAVA_HOME = Path(os.environ.get("JAVA_HOME") or "C:/Program Files/Eclipse Adoptium/jdk-17.0.19.10-hotspot")
JAVA = str(JAVA_HOME / "bin/java.exe")
JFR = str(JAVA_HOME / "bin/jfr.exe")


def sha1(p):
    return hashlib.sha1(p.read_bytes()).hexdigest()


def baixa(url, destino, soma):
    if destino.exists() and (not soma or destino.stat().st_size and sha1(destino) == soma):
        return
    destino.parent.mkdir(parents=True, exist_ok=True)
    for t in range(4):
        try:
            dados = urllib.request.urlopen(url, timeout=60).read()
            if soma and hashlib.sha1(dados).hexdigest() != soma:
                raise IOError("SHA-1 diferente: " + url)
            destino.write_bytes(dados)
            return
        except Exception:
            if t == 3:
                raise
            time.sleep(2)


def regra_ok(lib):
    ok = not lib.get("rules")
    for r in lib.get("rules", []):
        if r.get("os", {}).get("name", "windows") == "windows":
            ok = r["action"] == "allow"
    return ok


def prepara(forge=FORGE):
    marca = MC / ".pronto"
    bj = MC / "versions/1.20.1/1.20.1.json"
    if not bj.exists():
        man = json.loads(urllib.request.urlopen("https://piston-meta.mojang.com/mc/game/version_manifest_v2.json", timeout=60).read())
        v = next(v for v in man["versions"] if v["id"] == "1.20.1")
        baixa(v["url"], bj, v["sha1"])
    base = json.loads(bj.read_text("utf-8"))
    fj = json.loads((MC / f"versions/{forge}/{forge}.json").read_text("utf-8"))
    libs, vistos = [], set()
    for lib in fj["libraries"] + base["libraries"]:
        if not regra_ok(lib) or "natives-windows-arm64" in lib["name"] or "natives-windows-x86" in lib["name"]:
            continue
        art = lib["downloads"]["artifact"]
        partes = lib["name"].split(":")
        chave = (partes[0], partes[1], partes[3] if len(partes) > 3 else "")
        if chave in vistos:
            continue
        vistos.add(chave)
        libs.append((art["path"], art.get("url"), art.get("sha1")))
    if not marca.exists():
        tarefas = [(u, MC / "libraries" / p, s) for p, u, s in libs if u]
        cj = MC / "versions/1.20.1/1.20.1.jar"
        c = base["downloads"]["client"]
        tarefas.append((c["url"], cj, c["sha1"]))
        idx = base["assetIndex"]
        ip = MC / "assets/indexes" / (idx["id"] + ".json")
        baixa(idx["url"], ip, idx["sha1"])
        for o in json.loads(ip.read_text("utf-8"))["objects"].values():
            h = o["hash"]
            tarefas.append((f"https://resources.download.minecraft.net/{h[:2]}/{h}", MC / "assets/objects" / h[:2] / h, h))
        with cf.ThreadPoolExecutor(16) as ex:
            list(ex.map(lambda t: baixa(*t), tarefas))
        marca.write_text("ok")
    return base, fj, libs


def ram_livre_mb():
    class M(ctypes.Structure):
        _fields_ = [("l", ctypes.c_uint32), ("c", ctypes.c_uint32)] + [(n, ctypes.c_uint64) for n in "abcdefg"]
    m = M(); m.l = ctypes.sizeof(M)
    ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(m))
    return m.b // 2**20, m.d // 2**20  # física livre, commit livre


def comando(inst, xmx="4G", jvm=(), props=(), largura=1920, altura=1080, cheia=False, extra=(), forge=FORGE, java=JAVA):
    base, fj, libs = prepara(forge)
    L = str(MC / "libraries")
    cp = ";".join([str(MC / "libraries" / p) for p, _, _ in libs] + [str(MC / f"versions/{forge}/{forge}.jar")])
    troca = {"${version_name}": forge, "${library_directory}": L, "${classpath_separator}": ";"}
    def t(s):
        for k, v in troca.items():
            s = s.replace(k, v)
        return s
    nat = inst / "natives"; nat.mkdir(exist_ok=True)
    cmd = [java, f"-Xmx{xmx}", *jvm, f"-Djava.library.path={nat}", "-Dminecraft.launcher.brand=bancada"]
    cmd += [f"-D{p}" for p in props]
    cmd += [t(a) for a in fj["arguments"]["jvm"]] + ["-cp", cp, fj["mainClass"]]
    cmd += ["--username", "Bancada", "--version", forge, "--gameDir", str(inst), "--assetsDir", str(MC / "assets"),
            "--assetIndex", base["assetIndex"]["id"], "--uuid", "0" * 32, "--accessToken", "0", "--userType", "legacy",
            "--versionType", "release"]
    cmd += ["--fullscreen"] if cheia else []
    cmd += ["--width", str(largura), "--height", str(altura)]
    cmd += [t(a) for a in fj["arguments"]["game"]] + list(extra)
    return cmd


def janelas_travadas(pid):
    """Janelas do processo que o Windows marcou como 'não está respondendo' (as que ele pinta de branco)."""
    u = ctypes.windll.user32
    achadas = []
    @ctypes.WINFUNCTYPE(ctypes.c_bool, ctypes.c_void_p, ctypes.c_void_p)
    def cada(h, _):
        d = ctypes.c_ulong()
        u.GetWindowThreadProcessId(ctypes.c_void_p(h), ctypes.byref(d))
        if d.value == pid and u.IsWindowVisible(ctypes.c_void_p(h)) and u.IsHungAppWindow(ctypes.c_void_p(h)):
            achadas.append(h)
        return True
    u.EnumWindows(cada, None)
    return len(achadas)


def roda(inst, limite=900, env=None, saida=None, **kw):
    cmd = comando(inst, **kw)
    e = dict(os.environ); e.update(env or {})
    log = open(saida or inst / "saida.txt", "wb")
    # mantém o PC acordado só enquanto o teste roda
    ctypes.windll.kernel32.SetThreadExecutionState(0x80000003)
    p = subprocess.Popen(cmd, cwd=inst, env=e, stdout=log, stderr=subprocess.STDOUT, creationflags=0x08000000)
    (inst / "pid.txt").write_text(str(p.pid))
    arq, ini = Path(saida or inst / "saida.txt"), time.time()
    try:
        while time.time() - ini < limite:
            try:
                return p.wait(2)
            except subprocess.TimeoutExpired:
                pass
            if janelas_travadas(p.pid):
                with open(str(saida or inst / "saida.txt") + ".travada", "a") as f:
                    f.write(time.strftime("%H:%M:%S") + " janela sem responder\n")
            t = arq.read_text("utf-8", "replace") if arq.exists() else ""
            if "#@!@# Game crashed" in t or "Error during pre-loading phase" in t or "LoadingFailedException" in t or "Mod Loading has failed" in t:
                time.sleep(8)
                break
        p.kill(); p.wait()
        return 124
    finally:
        ctypes.windll.kernel32.SetThreadExecutionState(0x80000000)
        log.close()


if __name__ == "__main__":
    a = argparse.ArgumentParser()
    a.add_argument("--inst"); a.add_argument("--xmx", default="4G"); a.add_argument("--limite", type=int, default=900)
    a.add_argument("--prop", action="append", default=[]); a.add_argument("--env", action="append", default=[])
    a.add_argument("--jvm", action="append", default=[]); a.add_argument("--prepara", action="store_true")
    x = a.parse_args()
    if x.prepara:
        prepara(); print("pronto", ram_livre_mb()); sys.exit()
    inst = Path(x.inst).resolve(); inst.mkdir(parents=True, exist_ok=True)
    sys.exit(roda(inst, x.limite, dict(v.split("=", 1) for v in x.env), xmx=x.xmx, jvm=x.jvm, props=x.prop))
