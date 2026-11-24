# uso: testa_janela.sh modo [ENV=valor...]: abre a janela verde, lê a cor do ponto do meio da janela (só esse ponto da tela)
cd "$(dirname "$0")/.."
D="$(pwd -W)/${DLLS:-src/main/resources/niwvulkan/win64}"
("${JAVA_HOME:?}/bin/java" ${JV:-} -cp "bancada/janela/out;$(cat bancada/janela/cp.txt)" br.niw.vulkan.inicio.TesteJanela "$1" "$D" "${@:2}" > bancada/janela/saida.txt 2>&1 &)
sleep 4
powershell -NoProfile -c "Add-Type -AssemblyName System.Drawing; \$b=New-Object System.Drawing.Bitmap 1,1; \$g=[System.Drawing.Graphics]::FromImage(\$b); \$g.CopyFromScreen(420,280,0,0,(New-Object System.Drawing.Size 1,1)); 'cor no meio da janela: '+\$b.GetPixel(0,0)"
sleep 3; tail -4 bancada/janela/saida.txt
