"""Compara blocos de dois mundos (pastas region). uso: compara_mundo.py A B"""
import struct, sys, zlib, gzip
from pathlib import Path
import numpy as np

def nbt(b):
    p = 0
    def le(t):
        nonlocal p
        if t == 1: v = b[p]; p += 1; return v
        if t == 2: v = struct.unpack_from(">h", b, p)[0]; p += 2; return v
        if t == 3: v = struct.unpack_from(">i", b, p)[0]; p += 4; return v
        if t == 4: v = struct.unpack_from(">q", b, p)[0]; p += 8; return v
        if t == 5: p += 4; return 0
        if t == 6: p += 8; return 0
        if t == 7: n = struct.unpack_from(">i", b, p)[0]; p += 4 + n; return None
        if t == 8: n = struct.unpack_from(">H", b, p)[0]; p += 2; v = b[p:p + n].decode("utf-8", "replace"); p += n; return v
        if t == 9:
            k = b[p]; n = struct.unpack_from(">i", b, p + 1)[0]; p += 5
            return [le(k) for _ in range(n)]
        if t == 10:
            d = {}
            while True:
                k = b[p]; p += 1
                if k == 0: return d
                n = struct.unpack_from(">H", b, p)[0]; p += 2
                nome = b[p:p + n].decode(); p += n
                d[nome] = le(k)
        if t == 11: n = struct.unpack_from(">i", b, p)[0]; p += 4; v = np.frombuffer(b, ">i4", n, p); p += 4 * n; return v
        if t == 12: n = struct.unpack_from(">i", b, p)[0]; p += 4; v = np.frombuffer(b, ">u8", n, p); p += 8 * n; return v
    k = b[p]; p += 1
    n = struct.unpack_from(">H", b, p)[0]; p += 2 + n
    return le(k)

def chunks(pasta):
    r = {}
    for f in Path(pasta).glob("r.*.mca"):
        d = f.read_bytes()
        _, rx, rz, _ = f.name.split("."); rx, rz = int(rx), int(rz)
        if len(d) < 8192: continue
        for i in range(1024):
            o = struct.unpack_from(">I", d, i * 4)[0] >> 8
            if not o: continue
            n, c = struct.unpack_from(">IB", d, o * 4096)
            raw = d[o * 4096 + 5:o * 4096 + 4 + n]
            try: raw = zlib.decompress(raw) if c == 2 else gzip.decompress(raw)
            except Exception: continue
            r[(rx * 32 + i % 32, rz * 32 + i // 32)] = raw
    return r

def blocos(raw):
    t = nbt(raw)
    if t.get("Status") not in ("minecraft:full", "full"): return None
    out = {}
    for s in t["sections"]:
        bs = s.get("block_states")
        if not bs: continue
        pal = [x["Name"] + str(sorted(x.get("Properties", {}).items())) for x in bs["palette"]]
        if "data" not in bs: ids = np.zeros(4096, int)
        else:
            bits = max(4, (len(pal) - 1).bit_length()); por = 64 // bits
            dt = np.asarray(bs["data"], dtype=np.uint64)
            ids = np.concatenate([((dt >> np.uint64(k * bits)) & np.uint64((1 << bits) - 1))[:, None] for k in range(por)], 1).ravel()[:4096].astype(int)
        out[s["Y"] if s["Y"] < 128 else s["Y"] - 256] = np.array(pal, dtype=object)[ids]
    return out

a, b = chunks(sys.argv[1]), chunks(sys.argv[2])
comuns = sorted(set(a) & set(b))
tot = dif = cd = n = 0; nomes = {}
for k in comuns:
    x, y = blocos(a[k]), blocos(b[k])
    if x is None or y is None: continue
    n += 1; d0 = dif
    for s in set(x) & set(y):
        m = x[s] != y[s]; tot += 4096; q = int(m.sum()); dif += q
        if q:
            for u, v in zip(x[s][m][:200], y[s][m][:200]):
                c = (u.split("[")[0], v.split("[")[0]); nomes[c] = nomes.get(c, 0) + 1
    cd += dif > d0
print(f"{n} chunks completos em comum; blocos diferentes {dif} de {tot} ({dif/max(tot,1)*100:.5f}%), em {cd} chunks")
for c, q in sorted(nomes.items(), key=lambda z: -z[1])[:8]: print("   ", q, c)
