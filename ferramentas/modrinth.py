"""Baixa modpacks e mods do Modrinth para a bancada, sempre com SHA-512 conferido.
uso: modrinth.py pack SLUG PASTA | modrinth.py mods PASTA SLUG SLUG...
"""
import concurrent.futures as cf, hashlib, io, json, sys, urllib.parse, urllib.request, zipfile
from pathlib import Path

sys.stdout.reconfigure(encoding="utf-8", errors="replace")


def abre(u):
    return urllib.request.urlopen(urllib.request.Request(u, headers={"User-Agent": "niw-bancada/0.2"}), timeout=120).read()


def api(u):
    return json.loads(abre("https://api.modrinth.com/v2/" + u))


def versao(slug):
    q = "loaders=" + urllib.parse.quote('["forge"]') + "&game_versions=" + urllib.parse.quote('["1.20.1"]')
    v = api(f"project/{slug}/version?{q}")
    return v[0] if v else None


def grava(dest, dados, soma):
    if hashlib.sha512(dados).hexdigest() != soma:
        raise IOError("hash diferente: " + str(dest))
    dest.parent.mkdir(parents=True, exist_ok=True)
    dest.write_bytes(dados)


def dentro(base, rel):
    d = (base / rel).resolve()
    if base.resolve() not in d.parents:
        raise IOError("caminho fora da pasta: " + rel)
    return d


def pack(slug, pasta):
    v = versao(slug)
    f = next(x for x in v["files"] if x["filename"].endswith(".mrpack"))
    dados = abre(f["url"])
    assert hashlib.sha512(dados).hexdigest() == f["hashes"]["sha512"]
    z = zipfile.ZipFile(io.BytesIO(dados))
    ind = json.loads(z.read("modrinth.index.json"))
    print(v["version_number"], ind["dependencies"], len(ind["files"]), "arquivos")

    def um(a):
        if a.get("env", {}).get("client") == "unsupported":
            return
        d = dentro(pasta, a["path"])
        if not d.exists():
            grava(d, abre(a["downloads"][0]), a["hashes"]["sha512"])
    with cf.ThreadPoolExecutor(8) as ex:
        list(ex.map(um, ind["files"]))
    for n in z.namelist():
        for p in ("overrides/", "client-overrides/"):
            if n.startswith(p) and not n.endswith("/"):
                d = dentro(pasta, n[len(p):])
                d.parent.mkdir(parents=True, exist_ok=True)
                d.write_bytes(z.read(n))


def mods(pasta, slugs):
    vistos, fila = set(), list(slugs)
    existentes = [m.name.lower() for m in pasta.glob("*.jar")]
    while fila:
        s = fila.pop(0)
        if s in vistos:
            continue
        vistos.add(s)
        v = versao(s)
        if not v:
            print("sem versão forge 1.20.1:", s)
            continue
        f = next((x for x in v["files"] if x["primary"]), v["files"][0])
        raiz = f["filename"].lower().split("-")[0].split("_")[0]
        if not any(e.startswith(raiz) for e in existentes):
            grava(pasta / f["filename"], abre(f["url"]), f["hashes"]["sha512"])
            existentes.append(f["filename"].lower())
            print("baixado", f["filename"])
        fila += [d["project_id"] for d in v["dependencies"] if d["dependency_type"] == "required" and d["project_id"]]


if __name__ == "__main__":
    if sys.argv[1] == "pack":
        pack(sys.argv[2], Path(sys.argv[3]))
    else:
        mods(Path(sys.argv[2]), sys.argv[3:])
