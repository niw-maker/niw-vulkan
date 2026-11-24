import struct, sys
d = open(sys.argv[1], "rb").read()
pe = struct.unpack_from("<I", d, 0x3C)[0]
ns = struct.unpack_from("<H", d, pe + 6)[0]; so = struct.unpack_from("<H", d, pe + 20)[0]
opt = pe + 24; imp = struct.unpack_from("<I", d, opt + 112 + 8)[0]
secs = [struct.unpack_from("<IIII", d, opt + so + 40 * i + 8) for i in range(ns)]
def off(r):
    for vs, va, rs, po in secs:
        if va <= r < va + max(vs, rs): return po + r - va
o = off(imp)
while True:
    e = struct.unpack_from("<IIIII", d, o)
    if not e[3]: break
    s = off(e[3]); print(d[s:d.index(b"\0", s)].decode()); o += 20
