"""Roda medições A/B intercaladas na bancada.

uso: bancada.py --pack vanilla|p229|alien|cacu --variantes a,b --rodadas 3 [--segundos 25] [--rd 12] [--xmx 4G] [--fotos] [--jfr]
Espera jogos alheios (outro Claude, Rocket League, Minecraft do usuário) fecharem antes de cada rodada e descarta
rodadas em que um deles abriu no meio. Nunca encerra processo alheio.
"""
import argparse, json, re, shutil, statistics, subprocess, sys, time
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import lanca

R = lanca.RAIZ
B = R / "bancada"
# modpacks da sua máquina: ferramentas/packs.json (fora do git), ex.: {"meu": "C:/caminho/da/instancia"}; null = só a pasta da bancada
PACKS = {"vanilla": None, "shader": None, "bmc4": None, "heroi": None}
if (R / "ferramentas/packs.json").exists():
    PACKS |= {k: v and Path(v) for k, v in json.loads((R / "ferramentas/packs.json").read_text("utf-8")).items()}
OPCOES = """version:3465
enableVsync:false
maxFps:260
renderDistance:{rd}
simulationDistance:8
graphicsMode:1
ao:true
guiScale:3
fullscreen:{cheia}
pauseOnLostFocus:false
tutorialStep:none
onboardAccessibility:false
skipMultiplayerWarning:true
joinedFirstServer:true
lang:pt_br
narrator:0
mipmapLevels:4
biomeBlendRadius:2
renderClouds:"true"
soundCategory_master:0.0
soundCategory_music:0.0
rawMouseInput:false
"""
JOGOS = ("bootstraplauncher", "BootstrapLauncher", "net.minecraft", "forgeclient", "--gameDir", "devlaunch", "forge_gradle", "modmine", "arcanesymbols")


def alheios():
    ps = ("Get-CimInstance Win32_Process -Filter \"Name='java.exe' or Name='javaw.exe' or Name='RocketLeague.exe'\" | "
          "%{ \"$($_.ProcessId)|$($_.Name)|$($_.CommandLine)\" }")
    saida = subprocess.run(["powershell", "-NoProfile", "-c", ps], capture_output=True, text=True, errors="replace",
                           creationflags=0x08000000).stdout
    achados = []
    for l in saida.splitlines():
        p = l.split("|", 2)
        if len(p) < 3 or not p[0].isdigit() or str(R) in p[2]:
            continue
        if "RocketLeague" in p[1] or any(k in p[2] for k in JOGOS):
            achados.append((int(p[0]), p[2][:60]))
    return achados


def espera_livre(paz=40, max_s=6 * 3600):
    ini = time.time()
    livre, avisou = None, False
    while time.time() - ini < max_s:
        if alheios():
            livre = None
            if not avisou:
                print("esperando jogo alheio fechar...", flush=True)
                avisou = True
        elif livre is None:
            livre = time.time()
        elif time.time() - livre >= paz:
            return
        time.sleep(8)


def prepara_inst(pack):
    d = B / "inst" / pack
    d.mkdir(parents=True, exist_ok=True)
    marca = d / ".copiado"
    if not marca.exists():
        (d / "mods").mkdir(exist_ok=True)
        orig = PACKS[pack]
        if orig:
            for j in orig.glob("mods/*.jar"):
                if "tl_skin_cape" not in j.name:
                    shutil.copy2(j, d / "mods" / j.name)
            for sub in ("config", "defaultconfigs", "kubejs"):
                if (orig / sub).exists():
                    shutil.copytree(orig / sub, d / sub, dirs_exist_ok=True)
        marca.write_text("ok")
    return d


def usa(d, nome, quer, jar):
    alvo = d / "mods" / nome
    if quer:
        shutil.copy2(jar, alvo)
    elif alvo.exists():
        alvo.unlink()


# variante -> mod (usa o NiwVulkan), props, env, jvm, tira (mods retirados só nessa rodada)
VARIANTES = json.loads((R / "ferramentas/variantes.json").read_text("utf-8"))


def rodada(pack, var, i, a):
    v = VARIANTES[var]
    d = prepara_inst(pack)
    (d / "config").mkdir(exist_ok=True)
    (d / "options.txt").write_text(OPCOES.format(rd=a.rd, cheia="true" if a.cheia else "false"))
    fml = d / "config/fml.toml"
    t = fml.read_text() if fml.exists() else ""
    linha = f"earlyWindowControl = {'false' if v.get('semJanelaCedo', True) else 'true'}"
    t = re.sub(r"earlyWindowControl\s*=\s*\w+", linha, t) if "earlyWindowControl" in t else linha + "\n" + t
    fml.write_text(t)
    base, usoM = d / "mundo-base", d / "saves/medicao"
    pasta = B / "resultados" / a.nome
    pasta.mkdir(parents=True, exist_ok=True)
    for t in range(4):
        # o mundo da medição é sempre uma cópia do mesmo mundo-base
        shutil.rmtree(usoM, ignore_errors=True)
        if base.exists() and not a.novo:
            shutil.copytree(base, usoM)
        espera_livre(a.paz)
        usa(d, "niwmedidor.jar", True, R / "medidor/build/libs/niwmedidor-0.1.0.jar")
        usa(d, "niwvulkan.jar", v.get("mod", False), sorted((R / "build/libs").glob("niwvulkan-*.jar"))[-1])
        saida = pasta / f"{pack}-{var}-{i}.json"
        saida.unlink(missing_ok=True)
        for nome in v.get("tira", []):
            for j in (d / "mods").glob(nome):
                j.rename(j.with_name(j.name + ".fora"))
        ram = lanca.ram_livre_mb()
        rot = ("OTIMIZADO (NiwVulkan)" if v.get("mod") else "NORMAL (sem NiwVulkan)") + f" - {pack}/{var} rodada {i}"
        props = [f"niwmedidor.rotulo={rot}", f"niwmedidor.saida={saida}", f"niwmedidor.fases={a.fases}", f"niwmedidor.segundos={a.segundos}",
                 f"niwmedidor.fotos={'true' if a.fotos else 'false'}"] + v.get("props", []) + a.prop
        jvm = v.get("jvm", []) + ([f"-XX:StartFlightRecording=settings=profile,filename={saida}.jfr"] if a.jfr else [])
        ini = time.time()
        cod = lanca.roda(d, a.limite, dict(v.get("env", {})), xmx=a.xmx, jvm=jvm, props=props, cheia=a.cheia,
                         saida=str(saida) + ".log")
        intruso = alheios()
        for j in (d / "mods").glob("*.fora"):
            j.rename(j.with_name(j.name[:-5]))
        j = json.loads(saida.read_text("utf-8")) if saida.exists() else {}
        ok = bool(j.get("completo"))
        print(f"[{time.strftime('%H:%M:%S')}] {pack} {var} #{i} cod={cod} ok={ok} {time.time()-ini:.0f}s ram={ram} intruso={intruso}", flush=True)
        if j:
            j["ram_livre_mb"], j["commit_livre_mb"] = ram
            j["intruso"] = bool(intruso)
            saida.write_text(json.dumps(j, indent=1), "utf-8")
        if ok and not base.exists():
            shutil.copytree(usoM, base)
        if ok and (not intruso or a.aceita_intruso):
            return j
        if not intruso and t >= a.tentativas - 1:
            return None
    return None


def resumo(nome):
    pasta = B / "resultados" / nome
    grupos = {}
    for f in sorted(pasta.glob("*.json")):
        j = json.loads(f.read_text("utf-8"))
        if not j.get("completo"):
            continue
        pv = f.stem.rsplit("-", 1)[0]
        for fase, o in j.items():
            if isinstance(o, dict):
                grupos.setdefault(fase, {}).setdefault(pv, []).append(o)
    for fase, vs in grupos.items():
        print("==", fase)
        for pv, l in vs.items():
            def m(k):
                return statistics.median(x.get(k, 0) for x in l)
            print(f"  {pv:26} n={len(l)} fps={m('fps'):7.1f} [{min(x['fps'] for x in l):.0f}-{max(x['fps'] for x in l):.0f}]"
                  f" 1%={m('fps_1pc'):6.1f} 0.1%={m('fps_01pc'):6.1f} p99={m('p99_ms'):6.1f}ms trav={m('travadas_50ms'):.0f}"
                  f" mspt={m('mspt'):5.1f} nuc={m('nucleos_usados'):4.1f} gc={m('gc_ms'):.0f}ms sec={m('secoes'):.0f} chk={m('chunks_servidor'):.0f}")


if __name__ == "__main__":
    p = argparse.ArgumentParser()
    p.add_argument("--pack", default="vanilla")
    p.add_argument("--variantes", default="puro")
    p.add_argument("--rodadas", type=int, default=1)
    p.add_argument("--segundos", default="25")
    p.add_argument("--rd", type=int, default=12)
    p.add_argument("--xmx", default="4G")
    p.add_argument("--fases", default="carga,giro,horda,voo")
    p.add_argument("--nome", default="teste")
    p.add_argument("--limite", type=int, default=900)
    p.add_argument("--prop", action="append", default=[])
    p.add_argument("--tentativas", type=int, default=1)
    p.add_argument("--paz", type=int, default=40)
    for f in ("fotos", "jfr", "cheia", "novo", "resumo", "aceita_intruso"):
        p.add_argument("--" + f, action="store_true")
    a = p.parse_args()
    if not a.resumo:
        # uma medição por vez: a segunda espera a primeira soltar a trava
        import msvcrt
        trava = open(B / ".trava", "a+")
        while True:
            try:
                trava.seek(0)
                msvcrt.locking(trava.fileno(), msvcrt.LK_NBLCK, 1)
                break
            except OSError:
                time.sleep(5)
        for i in range(1, a.rodadas + 1):
            for v in a.variantes.split(","):
                rodada(a.pack, v, i, a)
    resumo(a.nome)
