"""Divide as amostras das threads de mundo por etapa. uso: etapas.py arq.jfr HH:MM:SS-HH:MM:SS"""
import re, collections, subprocess, sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent))
import lanca
R = Path(__file__).resolve().parent.parent
nomes = {}
for l in next((R / "build/fg_cache").rglob("srg_to_official_1.20.1.tsrg")).read_text(errors="replace").splitlines():
    p = l.split()
    if len(p) >= 3 and p[0].startswith("m_"): nomes[p[0]] = p[-1]
etapas = ["buildSurface", "applyCarvers", "applyBiomeDecoration", "doFill", "createStructures", "createReferences", "doCreateBiomes", "ThreadedLevelLightEngine", "LevelLightEngine", "RebuildTask", "ChunkSerializer", "updateRenderChunks", "spawnOriginalMobs"]
def seg(t):
    h, m, s = t.split(":"); return int(h) * 3600 + int(m) * 60 + float(s)
a, b = map(seg, sys.argv[2].split("-"))
src = subprocess.run([lanca.JFR, "print", "--events", "jdk.ExecutionSample", "--stack-depth", "64", sys.argv[1]], capture_output=True, text=True, errors="replace").stdout
n, c, o = 0, collections.Counter(), collections.Counter()
for bl in src.split("\njdk."):
    m = re.search(r'sampledThread = "([^"]+)"', bl); t = re.search(r"startTime = ([\d:.]+)", bl)
    if not m or not t or "Worker-Main" not in m[1] or not a <= seg(t[1]) <= b: continue
    f = re.findall(r"^\s+([\w.$<>]+)\(.*?\)\s+line:", bl, re.M)
    f = [q.split(".")[-2] + "." + nomes.get(q.split(".")[-1], q.split(".")[-1]) for q in f]
    n += 1; j = " ".join(f); e = next((x for x in etapas if x in j), "outros"); c[e] += 1
    if e == "outros": o[" < ".join(f[:3]) + " ... " + " < ".join(f[-16:-12])] += 1
print(n)
for k, v in c.most_common(): print(f"{v*100/n:5.1f}% {k}")
for k, v in o.most_common(6): print("   ", v, k)
