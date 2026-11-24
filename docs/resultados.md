# Resultados

23/11/2026, `ferramentas/bancada.py`: i7-10700, RTX 3060 (driver 610.88), 24 GB, Windows 11, 1920x1080, distância 12, sem limite de fps. Com e sem o mod intercalados, mesmo mundo, lugar e câmera. Valores: mediana de fps.

Cenas: **giro** (parado, câmera girando), **horda** (250 animais e 150 baús), **voo** (20 blocos/s em área nova), **bestiário** (até 150 mobs dos mods), **efeitos** (armaduras de mods, poderes do Palladium e 120 partículas por tick).

Até a 0.1.0 a janela em Vulkan saía branca (superfície transparente); os quadros eram desenhados e apresentados, mas sem aparecer. As linhas marcadas com * são de antes da correção. Refeito depois dela, o jogo puro deu 893 (giro) e 275 (horda), igual ao de antes.

## Jogo sem outros mods (3 rodadas)*
| cena | sem | com | ganho | 1% pior |
|---|---|---|---|---|
| giro | 282 | 877 | 3,1x | 125 → 249 |
| horda | 74 | 273 | 3,7x | 31 → 72 |
| voo | 147 | 649 | 4,4x | 50 → 88 |

## Heróis Palladium, 50+ mods e 1.266 poderes (0.3.0: 1 rodada; sem o mod: 2)
| cena | sem | com | ganho |
|---|---|---|---|
| giro | 60 | 157 | 2,6x |
| bestiário | 43 | 70 | 1,6x |
| efeitos | 18 | 25 | 1,4x |

Tick do servidor nos efeitos: 26 → 13 ms.

## Better MC 4, 365 arquivos, já com Xenon, Oculus, ImmediatelyFast, ModernFix e Radium (2 rodadas)
| cena | sem | com | ganho |
|---|---|---|---|
| giro | 330 | 472 | 1,4x |
| horda | 81 | 85 | 1,05x |
| bestiário | 41 | 50 | 1,2x |
| voo | 292 | 372 | 1,3x |

Na horda desse pack o 1% pior caiu de 18 para 13 fps: é o ponto fraco conhecido.

## Pack de 228 mods, sem Embeddium (3 rodadas)*
| cena | sem | com | ganho | tick do servidor |
|---|---|---|---|---|
| giro | 58 | 99 | 1,7x | 35 → 20 ms |
| horda | 25 | 50 | 2,0x | 56 → 42 ms |
| voo | 58 | 70 | 1,2x | 67 → 54 ms, com 6x mais terreno à vista |

## "alien real", 111 mods (3 rodadas*; linha da 0.3.0 com 1 rodada)
| cena | sem otimização | Embeddium+Oculus | NiwVulkan | os dois juntos |
|---|---|---|---|---|
| giro | 53 | 80 | 104 | 109 |
| horda | 21 | 33 | 41 | 41 |
| voo | 51 | 77 | 98 | 111 |

Com a 0.3.0 (Palladium): giro 73 (Embeddium+Oculus) → 117; horda 43 → 54.

## Outros (1 rodada)
- "cacu" (61 mods, ~20 otimizadores): giro 280 → 440, horda 100 → 161.
- Canary + Connector + ModernFix: giro 256 → 731, horda 88 → 203.
- Mekanism, Create, AE2, Ars, Aether (97 mods): giro 152 → 213.
- Shader Complementary em tela cheia: 162 → 250.

## Conferências
- Imagem na tela do PC (com autorização): janela, tela cheia, shader e pack de 111 mods.
- Mundo: diferença com o mod 0,5571%; entre duas gerações sem o mod 0,5564%.
- Bioma rápido: 0 diferenças em 40 milhões de testes.
- 30 minutos seguidos no "alien real": memória, handles e memória de vídeo estáveis.
- Recarga, distâncias 5/12/20, gráficos rápido/elegante/fabuloso, Nether, End, tempestade: sem erro.

## Sem ganho, retirado
`DontCompileHugeMethods`, juntar modelos na memória, coletor Shenandoah (um terço do fps), modos alternativos do Zink.
