# NiwVulkan (Minecraft 1.20.1 Forge)

Mod Forge que roda o jogo em Vulkan e distribui trabalho entre os núcleos **sem trocar o que os outros mods enxergam**.

## Regras do projeto
- Compatibilidade vem antes de desempenho: nada de trocar shader, formato de vértice ou evento do Forge; cada mudança dá o mesmo resultado do original.
- Todo módulo tem chave em `config/niwvulkan.properties` (`inicio/Chaves.java`) e o nome do mixin começa com o prefixo do módulo (`PluginMixin.chaveDe`). Conflito com outro mod desliga o módulo (`ErroMixin`), não derruba o jogo.
- Módulos de terreno, modelos, texturas e oclusão saem sozinhos com Embeddium/Rubidium/OptiFine; lotes saem com Oculus/ImmediatelyFast.
- Medir sempre A/B intercalado com `ferramentas/bancada.py`, lugar fixo, máquina livre. Ganho dentro do ruído: o módulo sai (já saíram: memória de modelos e compilação de métodos gigantes).
- Nunca ler a tela do PC: as fotos saem do framebuffer do próprio jogo.
- Nada da Mojang no repositório (`bancada/` fica fora do git).

## Estrutura
- `inicio/`: driver (Zink em `resources/niwvulkan/win64`), chaves, plugin de mixin.
- `terreno/`: `Regiao`/`Faixa` (buffers por região 8x4x8), `BufferArena` (VertexBuffer que mora na região), `Tabela` (resumo contíguo por seção), `Visao` (frustum em vetores), `Desenhista` (chamada múltipla).
- `entidades/`: `Lotes` (junta por RenderType original), `Oclusao`, escrita direta de vértices.
- `texturas/`: animações e atlas em vários núcleos. `mundo/`: biomas e busca de entidades. `Nucleos`: divide contas curtas do quadro.
- `medidor/`: mod de medição. `ferramentas/`: lançador, bancada, perfil de JFR, compilação do Mesa.

## Comandos
- Compilar: `JAVA_HOME=<JDK 17> ./gradlew build` → `build/libs/niwvulkan-*.jar`
- Medir: `python ferramentas/bancada.py --pack vanilla --variantes puro,niwVk --rodadas 3 --nome teste --fotos`
- Perfil: `python ferramentas/perfil.py arquivo.jfr main --hora HH:MM:SS-HH:MM:SS [--filhos Classe.metodo]`
- Zink: `ferramentas/compila_mesa.bat` (Visual Studio 2022, Meson, win_flex_bison; fontes em `C:\niwmesa`)
