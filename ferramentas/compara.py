"""Compara fotos internas de duas variantes. uso: compara.py pasta pack varA varB fase [saida.jpg]"""
import sys
import numpy as np
from PIL import Image
pasta, pack, a, b, fase = sys.argv[1:6]
x = np.array(Image.open(f"{pasta}/{pack}-{a}-1.json.{fase}.png").convert("RGB")).astype(int)
y = np.array(Image.open(f"{pasta}/{pack}-{b}-1.json.{fase}.png").convert("RGB")).astype(int)
d = np.abs(x - y).max(2)
print(f"{a} x {b} {fase}: media {d.mean():.3f}  >8: {(d>8).mean()*100:.3f}%  >40: {(d>40).mean()*100:.3f}%")
if len(sys.argv) > 6:
    h, w = d.shape
    c = Image.new("RGB", (w, h * 3 // 2))
    c.paste(Image.fromarray(x.astype("uint8")).resize((w // 2, h // 2)), (0, 0))
    c.paste(Image.fromarray(y.astype("uint8")).resize((w // 2, h // 2)), (w // 2, 0))
    c.paste(Image.fromarray(np.clip(d * 4, 0, 255).astype("uint8")).convert("RGB"), (0, h // 2))
    c.resize((w * 2 // 3, h)).save(sys.argv[6], quality=80)
