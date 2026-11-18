# Immersa 🥽

Immersa é um protótipo experimental Android VR (v0.1 Alpha).

## Recursos implementados
- OpenGL ES 2.0: renderização estereoscópica lado a lado.
- Head tracking por sensor de vetor de rotação, quando disponível.
- Painéis virtuais para até 12 aplicativos instalados.
- Seleção por olhar e abertura de aplicativos Android.
- Toque para recentralizar a direção horizontal.
- Site responsivo em `docs/index.html`.

## Limitações
- Aplicativos Android externos são abertos normalmente, **não** são capturados nem exibidos como janelas VR.
- Ainda não há correção de distorção óptica das lentes, calibração IPD ou controle Bluetooth.
- O rastreamento e a orientação dos painéis precisam de testes reais em aparelhos.
- APK de debug: não é uma versão final de produção.

## Build sem Android Studio
Acesse **Actions > Build Android APK > Run workflow**. O arquivo gerado fica em **Artifacts** da execução.
O site é publicado pelo workflow **Deploy Immersa Website** após habilitar GitHub Pages em **Settings > Pages > Source: GitHub Actions**.

## Downloads pelo site
O site procura automaticamente o APK anexado ao **GitHub Release** mais recente. Antes disso, aponta para a página de compilação do GitHub Actions. Os artefatos do Actions exigem login no GitHub para baixar.

## Tecnologia
Kotlin, Android SDK 35, OpenGL ES 2.0, Gradle 8.9, GitHub Actions, GitHub Pages.
