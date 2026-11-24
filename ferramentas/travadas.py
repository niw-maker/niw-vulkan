"""Mostra o que a thread de desenho fazia durante os quadros lentos. uso: travadas.py resultado.json fase [minimo_ms]"""
import collections, json, re, subprocess, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import lanca
arq, fase = sys.argv[1], sys.argv[2]
minimo = float(sys.argv[3]) if len(sys.argv) > 3 else 15
R = Path(__file__).resolve().parent.parent
nomes = {}
srg = next((R / "build/fg_cache").rglob("srg_to_official_1.20.1.tsrg"), None)
for l in srg.read_text(errors="replace").splitlines():
    p = l.split()
    if len(p) >= 3 and p[0].startswith("m_"):
        nomes[p[0]] = p[-1] if p[-1] != "static" else p[-2]
def seg(t):
    h, m, s = t.split(":")
    return int(h) * 3600 + int(m) * 60 + float(s)
lentos = [(seg(x.split()[0]), float(x.split()[1])) for x in json.load(open(arq, encoding="utf-8"))[fase]["lentos"]]
lentos = [(t, d) for t, d in lentos if d >= minimo]
print(len(lentos), "quadros lentos;", "soma", round(sum(d for _, d in lentos)), "ms")
J = lanca.JFR
def pega(ev):
    return subprocess.run([J, "print", "--events", ev, "--stack-depth", "40", arq + ".jfr"], capture_output=True, text=True, errors="replace").stdout
gc = []
for b in pega("jdk.GCPhasePause").split("\njdk."):
    t = re.search(r"startTime = ([\d:.]+)", b); d = re.search(r"duration = ([\d.]+) ms", b)
    if t and d: gc.append((seg(t[1]), float(d[1])))
am = []
for b in pega("jdk.ExecutionSample,jdk.NativeMethodSample").split("\njdk."):
    m = re.search(r'sampledThread = "([^"]+)"', b); t = re.search(r"startTime = ([\d:.]+)", b)
    if m and t and "main" == m[1]:
        pl = [q.split(".")[-2] + "." + nomes.get(q.split(".")[-1], q.split(".")[-1]) for q in re.findall(r"^\s+([\w.$<>]+)\(.*?\)\s+line:", b, re.M)]
        am.append((seg(t[1]), pl))
tot, topo, n, g = collections.Counter(), collections.Counter(), 0, 0
for t, d in lentos:
    a = t - d / 1000
    for x, y in gc:
        if a - 0.02 <= x <= t: g += min(y, d)
    for x, pl in am:
        if a <= x <= t and pl:
            n += 1; topo[pl[0]] += 1
            for q in set(pl): tot[q] += 1
print("pausas do coletor dentro delas:", round(g), "ms; amostras:", n)
for k, v in topo.most_common(14): print(f"  {v:4d} {k}")
print("-- com o que chama")
for k, v in tot.most_common(60):
    if not re.search(r"Launch|Method\.|Main\.|Accessor|Bootstrap", k): print(f"  {v:4d} {k}")
