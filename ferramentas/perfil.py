"""Resume um JFR: métodos mais quentes por thread, com nomes legíveis.
uso: perfil.py arquivo.jfr [thread] [--de seg --ate seg] [--pilha N] [--filtro texto]
"""
import argparse, collections, re, subprocess, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import lanca

R = Path(__file__).resolve().parent.parent
a = argparse.ArgumentParser()
a.add_argument("jfr"); a.add_argument("thread", nargs="?", default="main")
a.add_argument("--de", type=float, default=0); a.add_argument("--ate", type=float, default=1e9)
a.add_argument("--n", type=int, default=30); a.add_argument("--filtro", default=""); a.add_argument("--filhos", default="")
a.add_argument("--hora", default="", help="HH:MM:SS-HH:MM:SS")
x = a.parse_args()
nomes = {}
srg = next((R / "build/fg_cache").rglob("srg_to_official_1.20.1.tsrg"), None)
if srg:
    for l in srg.read_text(errors="replace").splitlines():
        p = l.split()
        if len(p) >= 3 and p[0].startswith("m_"):
            nomes[p[0]] = p[-1] if p[-1] != "static" else p[-2]
cache = Path(x.jfr + ".txt")
saida = cache.read_text("utf-8", "replace") if cache.exists() else subprocess.run([lanca.JFR, "print", "--events",
                        "jdk.ExecutionSample,jdk.NativeMethodSample", "--stack-depth", "40", x.jfr],
                       capture_output=True, text=True, errors="replace").stdout
if not cache.exists():
    cache.write_text(saida, "utf-8")
def seg(t):
    h, m, s = t.split(":")
    return int(h) * 3600 + int(m) * 60 + int(s)
hora = [seg(t) for t in x.hora.split("-")] if x.hora else None
topo, tot, n = collections.Counter(), collections.Counter(), 0
t0 = None
threads = collections.Counter()
filhos = collections.Counter()
for bloco in saida.split("\njdk.")[0:]:
    m = re.search(r'sampledThread = "([^"]+)"', bloco)
    t = re.search(r"startTime = (\d+):(\d+):([\d.]+)", bloco)
    if not m or not t:
        continue
    s = int(t[1]) * 3600 + int(t[2]) * 60 + float(t[3])
    t0 = s if t0 is None else t0
    if not x.de <= s - t0 <= x.ate:
        continue
    if hora and not hora[0] <= s <= hora[1]:
        continue
    threads[re.sub(r"[-#]?\d+$", "", m[1])] += 1
    if x.thread not in m[1]:
        continue
    pilha = re.findall(r"^\s+([\w.$<>]+)\(.*?\)\s+line:", bloco, re.M)
    pilha = [".".join(q.split(".")[-2:]) for q in pilha]
    pilha = [".".join([q.split(".")[0], nomes.get(q.split(".")[-1], q.split(".")[-1])]) if "." in q else q for q in pilha]
    if not pilha or (x.filtro and not any(x.filtro in q for q in pilha)):
        continue
    n += 1
    if x.filhos:
        for k in range(len(pilha) - 1, -1, -1):
            if x.filhos in pilha[k]:
                filhos[pilha[k - 1] if k else "(próprio)"] += 1
                break
    topo[pilha[0]] += 1
    for q in set(pilha):
        tot[q] += 1
if x.filhos:
    print("== chamados por", x.filhos, "de", n)
    for k, v in filhos.most_common(x.n):
        print(f"{v*100/max(n,1):5.1f}%  {k}")
    sys.exit()
print("amostras por thread:", threads.most_common(10))
print(f"== {x.thread}: {n} amostras\n-- próprio")
for k, v in topo.most_common(x.n):
    print(f"{v*100/max(n,1):5.1f}%  {k}")
print("-- total (com o que chama)")
for k, v in tot.most_common(x.n * 2):
    print(f"{v*100/max(n,1):5.1f}%  {k}")
