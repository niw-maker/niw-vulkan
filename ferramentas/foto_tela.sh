# Só com autorização do usuário: grava poucos quadros do monitor principal durante um teste. uso: foto_tela.sh saida.jpg
ffmpeg -hide_banner -loglevel error -y -f gdigrab -framerate 2 -t 1 -offset_x 0 -offset_y 0 -video_size 1920x1080 -i desktop -frames:v 1 -vf scale=1280:-2 "$1"
