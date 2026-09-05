# Wrote NOVIDADES.md
# Novidades do Client

## ShieldBreaker corrigido
- Antes era quase instantâneo mesmo na config mais lenta (bug: os delays eram sorteados a cada tick e todas as etapas disparavam juntas).
- Agora segue etapas de verdade: reação → troca pro machado → ataque → volta pro slot → cooldown.
- Delays respeitados (até 2000ms por etapa) + settings novas: `Cooldown MS`, `Swap Back`, `Require Axe`, `Wait Vanilla Cooldown`.
- A troca agora procura o machado só na hotbar.

## SkinChanger (novo, aba Misc)
- Copia a skin de qualquer nick (só visual, no seu jogo).
- Settings: `Skin Name`, `Model` (Auto/Classic/Slim) e `Cape`.
- Reaplica sozinho ~1s depois de editar. Precisa de internet (busca na API da Mojang).

## Notificações
- Card arredondado no canto inferior direito ao ativar/desativar qualquer módulo (verde = on, vermelho = off), com animação e barra de progresso.
- Desliga em `Client > Notifications`.

## UI arredondada
- Fundo, header, cards de módulo, categorias, barra de pesquisar (pílula com borda roxa ao focar), sliders, checkboxes, dropdowns, caixas de texto e tela de configs.
- Campos de texto funcionam nas settings (vale pro `Watermark Text` também).
- Corrigido bug que deixava retângulos de tom diferente no fundo.

## Build
- Requer JDK 21: `./gradlew build` — jar sai em `build/libs/`.
