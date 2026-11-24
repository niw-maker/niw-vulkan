# NiwVulkan

Mod para **Minecraft Java 1.20.1 + Forge 47** (Windows 64 bits) que roda o jogo em **Vulkan** e usa melhor os núcleos do processador, sem trocar o que os outros mods enxergam.

**[⬇ Baixar a última versão](https://github.com/niw-maker/niw-vulkan/releases/latest)**

> **Versão de teste.** Até agora só foi usado em uma máquina (i7-10700 + RTX 3060). Placas AMD e Intel ainda não foram testadas. **Faça cópia dos seus mundos antes.**

## Como instalar

1. Baixe o `niwvulkan-X.Y.Z.jar` e ponha na pasta `mods` da sua instância **Forge 1.20.1**. Se tiver uma versão antiga do mod, apague.
2. Abra o jogo, **feche e abra de novo**. Na primeira vez ele só desliga a janela de carregamento do Forge, que abre o OpenGL antes da hora.
3. Aperte F3: deve aparecer `zink Vulkan` na linha da placa de vídeo.

Só precisa no cliente; dá para entrar em servidores que não têm o mod. Para remover, apague o jar.

**Precisa de:** Windows 10/11 64 bits, Java 17, Forge 47.x e placa com Vulkan. Nada é instalado no Windows: o driver vem dentro do jar e só vale para o jogo.

## Quanto melhora

fps no mesmo lugar, com e sem o mod (mais em [docs/resultados.md](docs/resultados.md)):

| | Sem | Com |
|---|---|---|
| Jogo sem outros mods | 282 | **877** |
| 250 animais + 150 baús | 74 | **273** |
| Heróis Palladium | 60 | **157** |
| Modpack de 228 mods | 58 | **99** |
| Better MC 4, 365 arquivos, já otimizado | 330 | **472** |

Nem tudo melhora: na cena de 250 animais do Better MC 4 a média ficou quase igual (81 → 85) e os piores momentos pioraram.

## Deu problema?

1. Abra `config/niwvulkan.properties`, troque `vulkan=true` por `vulkan=false` e tente de novo.
2. Continua? Desligue as outras chaves, uma por vez, até achar a culpada.
3. Conte em **[Issues](https://github.com/niw-maker/niw-vulkan/issues/new/choose)** com o `logs/latest.log`.

O mod tem duas proteções: se o Vulkan não conseguir abrir a janela, o jogo volta ao OpenGL na abertura seguinte; se uma parte não encaixar com outro mod, só ela é desligada.

| Chave | O que faz |
|---|---|
| `vulkan` | Troca o OpenGL pelo Vulkan |
| `terreno` | Desenha o mundo em poucas chamadas |
| `lotes`, `modelos` | Junta e acelera o desenho de entidades |
| `oclusao` | Não desenha mobs escondidos atrás de paredes |
| `texturas` | Anima texturas em vários núcleos |
| `geracao`, `mundo` | Aceleram a geração e o mundo |
| `entidades` | Alivia dados de entidades e do jogador |
| `palladium` | Evita reavaliar animações vazias do Palladium |
| `prioridade` | Desenho passa na frente das threads de mundo |

## Compatibilidade

- **Embeddium, Xenon e Oculus:** funcionam junto. O mod desliga sozinho as partes que eles já fazem. Shaders testados: Complementary Unbound.
- **ImmediatelyFast, ModernFix, Canary, Radium, EntityCulling, FerriteCore:** testados junto.
- **Nvidium:** não funciona com o Vulkan ligado.
- **OptiFine e VulkanMod:** não testados.

Compatibilidade com todos os mods não dá para garantir. Foram testados 8 pacotes, de 8 a 365 mods.

## Como funciona

Outros mods de Vulkan trocam o renderizador, e por isso quebram shaders e mods que desenham por conta própria. Aqui o jogo e os mods continuam falando OpenGL; o que muda é o que está embaixo: o [Zink, do Mesa](https://docs.mesa3d.org/drivers/zink.html), um OpenGL que executa em Vulkan. Por cima disso, o mod reduz o trabalho do processador sem mudar shaders nem formato de vértices.

O tick das entidades continua em uma thread, de propósito: os mods contam com isso, e dividir corromperia mundos.

## Compilar

`./gradlew build` com JDK 17; o jar sai em `build/libs`. O Zink foi compilado de `mesa-26.3.1` com `ferramentas/mesa.patch` e `ferramentas/compila_mesa.bat`. As ferramentas de medição estão em `ferramentas/` e `medidor/`.

## Licenças

Código: [MIT](LICENSE). Zink/Mesa e zlib: textos em [licencas/](licencas), também dentro do jar. Nenhum arquivo do Minecraft faz parte deste repositório. Projeto de fã, sem ligação com a Mojang ou a Microsoft.
